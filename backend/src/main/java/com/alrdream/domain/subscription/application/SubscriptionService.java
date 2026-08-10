package com.alrdream.domain.subscription.application;

import com.alrdream.domain.subscription.domain.Subscription;
import com.alrdream.domain.subscription.domain.SubscriptionRepository;
import com.alrdream.domain.subscription.domain.SubscriptionStatus;
import com.alrdream.infrastructure.payment.PortOnePaymentException;
import io.portone.sdk.server.common.Currency;
import io.portone.sdk.server.common.PaymentAmountInput;
import io.portone.sdk.server.payment.PaymentClient;
import io.portone.sdk.server.payment.paymentschedule.BillingKeyPaymentScheduleInput;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * [01] 13번, [03] §4-7 — 빌링키를 받아 최초 결제를 즉시 요청하고 다음 달 결제를 예약한다. 실제 결제 성공/실패는
 * 웹훅으로만 확정되므로({@link PortOneWebhookService}) 여기서는 구독을 {@code PAST_DUE}(대기) 상태로 만들고
 * PortOne에 결제/예약 요청을 보내는 것까지만 한다.
 *
 * <p>구독 생성(DB 쓰기)과 결제 요청(외부 호출, 되돌릴 수 없음)을 하나의 {@code @Transactional} 메서드에 함께
 * 두면 안 된다 — 결제 자체는 성공했는데 그 뒤 단계(예: 다음 달 결제 예약)에서 예외가 나 트랜잭션이 롤백되면,
 * 이미 카드가 결제됐는데 그 결제를 가리킬 {@code Subscription} 행이 사라져 웹훅이 "존재하지 않는 구독"으로
 * 무시해버리는 사고가 난다. 그래서 컨트롤러가 아래 3개의 독립적인 트랜잭션 메서드를 순서대로 호출하는 방식으로
 * 나눴다(같은 빈 안에서 {@code this.xxx()} 자가 호출로는 프록시를 거치지 않아 {@code @Transactional}이 무시되므로
 * — PortOneWebhookService의 동일 문제 주석 참고 — 반드시 외부(컨트롤러)에서 호출해야 한다):
 * {@link #createPendingSubscription} → (컨트롤러가 PortOne 결제 호출) → 실패 시 {@link #cancelSubscription},
 * 성공 시 (컨트롤러가 다음 결제 예약 호출 후) {@link #finalizeSubscription}.
 */
@Service
public class SubscriptionService {

	private static final String ORDER_NAME = "알려드림 Pro 구독";

	private final SubscriptionRepository subscriptionRepository;
	private final PaymentClient paymentClient;
	private final String channelKey;
	private final long proMonthlyPriceKrw;

	public SubscriptionService(
			SubscriptionRepository subscriptionRepository,
			PaymentClient paymentClient,
			@Value("${app.portone.channel-key}") String channelKey,
			@Value("${app.portone.pro-monthly-price-krw}") long proMonthlyPriceKrw) {
		this.subscriptionRepository = subscriptionRepository;
		this.paymentClient = paymentClient;
		this.channelKey = channelKey;
		this.proMonthlyPriceKrw = proMonthlyPriceKrw;
	}

	/** 중복 구독 여부를 확인하고 {@code PAST_DUE} 상태의 구독 행을 커밋한다 — 이 뒤에야 실제 결제를 요청한다. */
	@Transactional
	public Subscription createPendingSubscription(UUID userId, String billingKeyId) {
		subscriptionRepository.findFirstByUserIdOrderByStartedAtDesc(userId).ifPresent(existing -> {
			if (existing.getStatus() != SubscriptionStatus.CANCELED) {
				throw new IllegalArgumentException("이미 구독 중입니다.");
			}
		});
		return subscriptionRepository.save(Subscription.create(userId, billingKeyId));
	}

	/** 최초 결제 요청 자체가 실패해(돈이 오가지 않음) 되돌릴 때만 호출한다 — 재구독을 막지 않도록 CANCELED로 정리. */
	@Transactional
	public void cancelSubscription(UUID subscriptionId) {
		subscriptionRepository.findById(subscriptionId).ifPresent(Subscription::cancel);
	}

	/** 결제/다음 달 예약까지 모두 성공한 뒤에만 호출 — 다음 결제 예정일을 확정 저장한다. */
	@Transactional
	public Subscription finalizeSubscription(UUID subscriptionId, OffsetDateTime nextBillingAt) {
		Subscription subscription = subscriptionRepository.findById(subscriptionId)
				.orElseThrow(() -> new IllegalArgumentException("구독을 찾을 수 없습니다."));
		subscription.scheduleNextBilling(nextBillingAt);
		return subscription;
	}

	@Transactional(readOnly = true)
	public Subscription getCurrent(UUID userId) {
		return subscriptionRepository.findFirstByUserIdOrderByStartedAtDesc(userId)
				.orElseThrow(() -> new IllegalArgumentException("구독 내역이 없습니다."));
	}

	/** 컨트롤러가 최초 결제를 요청할 때 사용 — 실패 시 {@link IllegalArgumentException}으로 감싸 던진다. */
	public void chargeFirstPayment(UUID subscriptionId, String billingKeyId) {
		PaymentAmountInput amount = new PaymentAmountInput(proMonthlyPriceKrw, null, null);
		String firstPaymentId = PaymentIds.generate(subscriptionId);
		try {
			paymentClient.payWithBillingKey(
							firstPaymentId, billingKeyId, channelKey, ORDER_NAME, null, null,
							amount, Currency.Krw.INSTANCE, null, null, null, null, null,
							null, null, null, null, null, null, null, null, null)
					.join();
		} catch (RuntimeException e) {
			throw new IllegalArgumentException("결제 요청에 실패했습니다: " + rootMessage(e), e);
		}
	}

	/** 컨트롤러가 다음 달 결제를 예약할 때 사용 — 실패해도 이미 첫 결제가 승인된 뒤라 구독 자체는 유지해야 한다. */
	public void scheduleNextPayment(UUID subscriptionId, String billingKeyId, OffsetDateTime nextBillingAt) {
		PaymentAmountInput amount = new PaymentAmountInput(proMonthlyPriceKrw, null, null);
		String nextPaymentId = PaymentIds.generate(subscriptionId);
		BillingKeyPaymentScheduleInput scheduleInput = new BillingKeyPaymentScheduleInput(
				null, billingKeyId, channelKey, ORDER_NAME, null, null, amount, Currency.Krw.INSTANCE,
				null, null, null, null, null, null, null, null, null, null, null, null, null);
		try {
			paymentClient.getPaymentSchedule()
					.createPaymentSchedule(nextPaymentId, scheduleInput, nextBillingAt.toInstant())
					.join();
		} catch (RuntimeException e) {
			throw new PortOnePaymentException("다음 달 결제 예약에 실패했습니다: " + rootMessage(e), e);
		}
	}

	// PortOne SDK의 일부 예외(예: BillingKeyNotFoundException)는 message가 비어 있을 수 있어(PortOne API
	// 응답 자체에 메시지가 없는 경우), 그런 경우 예외 클래스 이름으로라도 원인을 알 수 있게 폴백한다.
	static String rootMessage(Throwable e) {
		Throwable cause = e.getCause() != null ? e.getCause() : e;
		return cause.getMessage() != null ? cause.getMessage() : cause.getClass().getSimpleName();
	}
}
