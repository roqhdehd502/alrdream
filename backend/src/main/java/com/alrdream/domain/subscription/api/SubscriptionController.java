package com.alrdream.domain.subscription.api;

import com.alrdream.domain.subscription.api.dto.CreateSubscriptionRequest;
import com.alrdream.domain.subscription.api.dto.PaymentHistoryResponse;
import com.alrdream.domain.subscription.api.dto.PricingResponse;
import com.alrdream.domain.subscription.api.dto.SubscriptionResponse;
import com.alrdream.domain.subscription.domain.Subscription;
import com.alrdream.domain.subscription.application.SubscriptionPricingService;
import com.alrdream.domain.subscription.application.SubscriptionService;
import com.alrdream.global.error.ErrorResponse;
import com.alrdream.global.security.MemberPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Subscription", description = "구독/결제 — PortOne V2 빌링키 정기결제 [01] 13번, [03] §4-7")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/subscriptions")
public class SubscriptionController {

	private static final Logger log = LoggerFactory.getLogger(SubscriptionController.class);

	private final SubscriptionService subscriptionService;
	private final SubscriptionPricingService subscriptionPricingService;

	public SubscriptionController(
			SubscriptionService subscriptionService, SubscriptionPricingService subscriptionPricingService) {
		this.subscriptionService = subscriptionService;
		this.subscriptionPricingService = subscriptionPricingService;
	}

	@Operation(summary = "Pro 구독 상품 가격 조회", description = "구독 상품 소개 화면에서 혜택/가격을 보여줄 때 쓴다. 프로모션이 진행 중이면 그 가격을 함께 반환한다.")
	@ApiResponse(responseCode = "200", description = "조회 성공")
	@GetMapping("/pricing")
	public ResponseEntity<PricingResponse> getPricing() {
		return ResponseEntity.ok(PricingResponse.of(subscriptionPricingService.get()));
	}

	@Operation(
			summary = "Pro 구독 시작",
			description = "Frontend/Admin이 PortOne SDK로 발급받은 빌링키를 암호화 저장하고, 최초 결제를 즉시 요청한 뒤 "
					+ "다음 달 결제를 예약한다. 실제 결제 성공/실패는 웹훅(POST /webhooks/portone)으로만 확정되므로 "
					+ "응답의 status는 아직 PAST_DUE(대기)다.")
	@ApiResponse(responseCode = "200", description = "결제 요청/예약 성공 (status는 아직 PAST_DUE)")
	@ApiResponse(responseCode = "400", description = "이미 구독 중이거나 PortOne 결제 요청이 거절됨",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@PostMapping
	public ResponseEntity<SubscriptionResponse> subscribe(
			@AuthenticationPrincipal MemberPrincipal principal, @Valid @RequestBody CreateSubscriptionRequest request) {
		String billingKeyId = request.billingKeyId();
		Subscription subscription =
				subscriptionService.createPendingSubscription(principal.memberId(), billingKeyId);
		UUID subscriptionId = subscription.getId();

		try {
			subscriptionService.chargeFirstPayment(subscriptionId, billingKeyId);
		} catch (RuntimeException e) {
			// 결제 자체가 실패해 돈이 오가지 않았으므로 안전하게 되돌린다(재구독 가능하도록 CANCELED 처리).
			subscriptionService.cancelSubscription(subscriptionId);
			throw e;
		}

		// 여기서부터는 이미 카드가 결제된 뒤다 — 다음 결제 예약이 실패하더라도 절대 구독 행을 지우거나 되돌리면
		// 안 된다(웹훅이 이 행을 찾아 결제를 확정 처리해야 한다). 예약은 웹훅의 Transaction.Paid 처리에서도
		// 한 번 더 시도되므로(PortOneWebhookService#handlePaid), 여기서 실패해도 완전히 유실되지는 않는다.
		OffsetDateTime nextBillingAt = OffsetDateTime.now().plusMonths(1);
		String scheduleId;
		try {
			scheduleId = subscriptionService.scheduleNextPayment(subscriptionId, billingKeyId, nextBillingAt);
		} catch (RuntimeException e) {
			log.warn("다음 달 결제 예약에 실패했습니다(구독은 유지, 웹훅에서 재시도됨): subscriptionId={}", subscriptionId, e);
			return ResponseEntity.ok(SubscriptionResponse.of(subscription));
		}

		return ResponseEntity.ok(SubscriptionResponse.of(
				subscriptionService.finalizeSubscription(subscriptionId, nextBillingAt, scheduleId)));
	}

	@Operation(summary = "내 구독 상태 조회")
	@ApiResponse(responseCode = "200", description = "조회 성공")
	@ApiResponse(responseCode = "400", description = "구독 내역이 없음",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@GetMapping("/me")
	public ResponseEntity<SubscriptionResponse> getCurrent(@AuthenticationPrincipal MemberPrincipal principal) {
		return ResponseEntity.ok(SubscriptionResponse.of(subscriptionService.getCurrent(principal.memberId())));
	}

	@Operation(
			summary = "Pro 구독 해지",
			description = "PortOne에 등록된 다음 결제 예약을 취소한 뒤 즉시 해지 처리한다(남은 기간 일할 환불 없음, 즉시 Free로 전환).")
	@ApiResponse(responseCode = "200", description = "해지 성공")
	@ApiResponse(responseCode = "400", description = "구독 내역이 없거나 이미 해지됨",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@DeleteMapping("/me")
	public ResponseEntity<SubscriptionResponse> cancel(@AuthenticationPrincipal MemberPrincipal principal) {
		Subscription subscription = subscriptionService.revokeNextPaymentSchedule(principal.memberId());
		return ResponseEntity.ok(SubscriptionResponse.of(
				subscriptionService.finalizeCancelation(subscription.getId(), principal.memberId())));
	}

	@Operation(summary = "내 결제 내역 조회", description = "해지 후 재구독으로 여러 구독 이력이 있어도 전체 결제 내역을 최신순으로 반환한다.")
	@ApiResponse(responseCode = "200", description = "조회 성공")
	@GetMapping("/me/payments")
	public ResponseEntity<List<PaymentHistoryResponse>> getPaymentHistory(
			@AuthenticationPrincipal MemberPrincipal principal) {
		return ResponseEntity.ok(subscriptionService.getPaymentHistory(principal.memberId()).stream()
				.map(PaymentHistoryResponse::of)
				.toList());
	}
}
