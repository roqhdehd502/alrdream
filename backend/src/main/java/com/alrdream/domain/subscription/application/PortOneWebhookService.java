package com.alrdream.domain.subscription.application;

import com.alrdream.domain.member.domain.Member;
import com.alrdream.domain.member.domain.MemberPlan;
import com.alrdream.domain.member.domain.MemberRepository;
import com.alrdream.domain.subscription.domain.PaymentHistory;
import com.alrdream.domain.subscription.domain.PaymentHistoryRepository;
import com.alrdream.domain.subscription.domain.PaymentStatus;
import com.alrdream.domain.subscription.domain.Subscription;
import com.alrdream.domain.subscription.domain.SubscriptionRepository;
import io.portone.sdk.server.common.Currency;
import io.portone.sdk.server.common.PaymentAmountInput;
import io.portone.sdk.server.errors.WebhookVerificationException;
import io.portone.sdk.server.payment.Payment;
import io.portone.sdk.server.payment.PaymentClient;
import io.portone.sdk.server.payment.paymentschedule.BillingKeyPaymentScheduleInput;
import io.portone.sdk.server.webhook.Webhook;
import io.portone.sdk.server.webhook.WebhookTransactionDataFailed;
import io.portone.sdk.server.webhook.WebhookTransactionDataPaid;
import io.portone.sdk.server.webhook.WebhookTransactionFailed;
import io.portone.sdk.server.webhook.WebhookTransactionPaid;
import io.portone.sdk.server.webhook.WebhookVerifier;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * [03] §4-7 {@code POST /webhooks/portone} 처리. PortOne이 실패 시 최대 5회 재전송하므로
 * {@code payment_history.payment_id} 존재 여부로 멱등 처리한다. 웹훅 바디는 결제 성공/실패만 알려줄 뿐 금액을
 * 담지 않아, 확정된 금액은 {@link PaymentClient#getPayment}로 별도 조회해 신뢰한다(웹훅 바디 자체는 위변조
 * 가능성이 있어 서명 검증 외에는 최소한만 신뢰).
 */
@Service
public class PortOneWebhookService {

	private static final Logger log = LoggerFactory.getLogger(PortOneWebhookService.class);
	private static final String ORDER_NAME = "알려드림 Pro 구독";

	private final PaymentClient paymentClient;
	private final SubscriptionRepository subscriptionRepository;
	private final PaymentHistoryRepository paymentHistoryRepository;
	private final MemberRepository memberRepository;
	private final String webhookSecret;
	private final String channelKey;
	private final long proMonthlyPriceKrw;

	public PortOneWebhookService(
			PaymentClient paymentClient,
			SubscriptionRepository subscriptionRepository,
			PaymentHistoryRepository paymentHistoryRepository,
			MemberRepository memberRepository,
			@Value("${app.portone.webhook-secret}") String webhookSecret,
			@Value("${app.portone.channel-key}") String channelKey,
			@Value("${app.portone.pro-monthly-price-krw}") long proMonthlyPriceKrw) {
		this.paymentClient = paymentClient;
		this.subscriptionRepository = subscriptionRepository;
		this.paymentHistoryRepository = paymentHistoryRepository;
		this.memberRepository = memberRepository;
		this.webhookSecret = webhookSecret;
		this.channelKey = channelKey;
		this.proMonthlyPriceKrw = proMonthlyPriceKrw;
	}

	/**
	 * 컨트롤러가 호출하는 유일한 외부 진입점이라 트랜잭션 경계를 여기 둔다 — {@link #handlePaid}/{@link #handleFailed}는
	 * {@code this.xxx()}로 자가 호출되므로 그 메서드들에 {@code @Transactional}을 달아도 Spring 프록시를 거치지
	 * 않아 적용되지 않는다(Planning/Analysis/Design의 Job 완료 처리와는 다른 이유로 겪은 동일한 함정 — 거긴
	 * 아예 트랜잭션 밖에서 실행돼 리포지토리 콜 자체의 경계에 맡겼지만, 여긴 호출자가 트랜잭션 안이라 착각하기
	 * 쉬웠다. 처음엔 이 메서드가 없어 handlePaid/handleFailed의 @Transactional이 무시된 채 클래스 레벨
	 * readOnly=true 트랜잭션 안에서 실행됐고, payment_history INSERT가 조용히 유실되는 버그가 있었다).
	 *
	 * @throws WebhookVerificationException 서명이 유효하지 않을 때 (컨트롤러에서 400 처리)
	 */
	@Transactional
	public void handle(String rawBody, String webhookId, String signature, String timestamp)
			throws WebhookVerificationException {
		Webhook webhook = new WebhookVerifier(webhookSecret).verify(rawBody, webhookId, signature, timestamp);
		if (webhook instanceof WebhookTransactionPaid paid) {
			handlePaid(paid.getData());
		} else if (webhook instanceof WebhookTransactionFailed failed) {
			handleFailed(failed.getData());
		} else {
			log.info("처리하지 않는 PortOne 웹훅 타입을 수신했습니다: {}", webhook.getClass().getSimpleName());
		}
	}

	private void handlePaid(WebhookTransactionDataPaid data) {
		String paymentId = data.getPaymentId();
		if (paymentHistoryRepository.existsByPaymentId(paymentId)) {
			log.info("이미 처리된 결제 웹훅입니다(중복 수신): paymentId={}", paymentId);
			return;
		}

		UUID subscriptionId = PaymentIds.parseSubscriptionId(paymentId);
		Subscription subscription = subscriptionRepository.findById(subscriptionId).orElse(null);
		if (subscription == null) {
			log.warn("웹훅이 가리키는 구독을 찾을 수 없습니다: paymentId={}, subscriptionId={}", paymentId, subscriptionId);
			return;
		}

		long amount = fetchAmount(paymentId);
		paymentHistoryRepository.save(
				PaymentHistory.create(subscriptionId, paymentId, amount, PaymentStatus.PAID, OffsetDateTime.now()));

		OffsetDateTime nextBillingAt = OffsetDateTime.now().plusMonths(1);
		subscription.activate(nextBillingAt);

		memberRepository.findById(subscription.getUserId()).ifPresent(member -> {
			member.changePlan(MemberPlan.PRO);
			memberRepository.save(member);
		});

		// [03] §4-7 "다음 달 결제 재예약(반복 체이닝)" — 매 결제 성공마다 그 다음 회차를 다시 예약한다.
		rescheduleNextPayment(subscriptionId, subscription.getBillingKey(), nextBillingAt);
	}

	private void handleFailed(WebhookTransactionDataFailed data) {
		String paymentId = data.getPaymentId();
		if (paymentHistoryRepository.existsByPaymentId(paymentId)) {
			log.info("이미 처리된 결제 웹훅입니다(중복 수신): paymentId={}", paymentId);
			return;
		}

		UUID subscriptionId = PaymentIds.parseSubscriptionId(paymentId);
		Subscription subscription = subscriptionRepository.findById(subscriptionId).orElse(null);
		if (subscription == null) {
			log.warn("웹훅이 가리키는 구독을 찾을 수 없습니다: paymentId={}, subscriptionId={}", paymentId, subscriptionId);
			return;
		}

		paymentHistoryRepository.save(
				PaymentHistory.create(subscriptionId, paymentId, 0L, PaymentStatus.FAILED, null));
		subscription.markPastDue();

		// 결제 실패 시 즉시 Pro 권한을 회수한다 — 재시도/알림 정책은 [03] §4-7에 구체적으로 명시돼 있지 않아
		// 이번 phase 스코프에서는 다루지 않는다(04_milestone.md 설계 결정 참고).
		memberRepository.findById(subscription.getUserId()).ifPresent(member -> {
			member.changePlan(MemberPlan.FREE);
			memberRepository.save(member);
		});
	}

	private long fetchAmount(String paymentId) {
		Payment payment = paymentClient.getPayment(paymentId).join();
		if (payment instanceof Payment.Recognized recognized) {
			return recognized.getAmount().getTotal();
		}
		log.warn("PortOne이 알 수 없는 결제 상태를 반환했습니다: paymentId={}", paymentId);
		return 0L;
	}

	private void rescheduleNextPayment(UUID subscriptionId, String billingKey, OffsetDateTime nextBillingAt) {
		String nextPaymentId = PaymentIds.generate(subscriptionId);
		PaymentAmountInput amount = new PaymentAmountInput(proMonthlyPriceKrw, null, null);
		BillingKeyPaymentScheduleInput scheduleInput = new BillingKeyPaymentScheduleInput(
				null, billingKey, channelKey, ORDER_NAME, null, null, amount, Currency.Krw.INSTANCE,
				null, null, null, null, null, null, null, null, null, null, null, null, null);
		paymentClient.getPaymentSchedule()
				.createPaymentSchedule(nextPaymentId, scheduleInput, nextBillingAt.toInstant())
				.join();
	}
}
