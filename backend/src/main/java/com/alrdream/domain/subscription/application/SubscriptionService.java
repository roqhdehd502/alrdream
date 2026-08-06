package com.alrdream.domain.subscription.application;

import com.alrdream.domain.subscription.domain.Subscription;
import com.alrdream.domain.subscription.domain.SubscriptionRepository;
import com.alrdream.domain.subscription.domain.SubscriptionStatus;
import com.alrdream.infrastructure.payment.PortOnePaymentException;
import io.portone.sdk.server.common.Currency;
import io.portone.sdk.server.common.PaymentAmountInput;
import io.portone.sdk.server.payment.PaymentClient;
import io.portone.sdk.server.payment.paymentschedule.BillingKeyPaymentScheduleInput;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * [01] 13번, [03] §4-7 — 빌링키를 받아 최초 결제를 즉시 요청하고 다음 달 결제를 예약한다. 실제 결제 성공/실패는
 * 웹훅으로만 확정되므로({@link PortOneWebhookService}) 여기서는 구독을 {@code PAST_DUE}(대기) 상태로 만들고
 * PortOne에 결제/예약 요청을 보내는 것까지만 한다.
 */
@Service
@Transactional(readOnly = true)
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

	@Transactional
	public Subscription subscribe(UUID userId, String billingKeyId) {
		subscriptionRepository.findFirstByUserIdOrderByStartedAtDesc(userId).ifPresent(existing -> {
			if (existing.getStatus() != SubscriptionStatus.CANCELED) {
				throw new IllegalArgumentException("이미 구독 중입니다.");
			}
		});

		Subscription subscription = subscriptionRepository.save(Subscription.create(userId, billingKeyId));
		UUID subscriptionId = subscription.getId();
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

		OffsetDateTime nextBillingAt = OffsetDateTime.now().plusMonths(1);
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

		subscription.scheduleNextBilling(nextBillingAt);
		return subscription;
	}

	public Subscription getCurrent(UUID userId) {
		return subscriptionRepository.findFirstByUserIdOrderByStartedAtDesc(userId)
				.orElseThrow(() -> new IllegalArgumentException("구독 내역이 없습니다."));
	}

	// PortOne SDK의 일부 예외(예: BillingKeyNotFoundException)는 message가 비어 있을 수 있어(PortOne API
	// 응답 자체에 메시지가 없는 경우), 그런 경우 예외 클래스 이름으로라도 원인을 알 수 있게 폴백한다.
	static String rootMessage(Throwable e) {
		Throwable cause = e.getCause() != null ? e.getCause() : e;
		return cause.getMessage() != null ? cause.getMessage() : cause.getClass().getSimpleName();
	}
}
