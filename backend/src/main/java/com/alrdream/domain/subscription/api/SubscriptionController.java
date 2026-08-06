package com.alrdream.domain.subscription.api;

import com.alrdream.domain.subscription.api.dto.CreateSubscriptionRequest;
import com.alrdream.domain.subscription.api.dto.SubscriptionResponse;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

	private final SubscriptionService subscriptionService;

	public SubscriptionController(SubscriptionService subscriptionService) {
		this.subscriptionService = subscriptionService;
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
		return ResponseEntity.ok(
				SubscriptionResponse.of(subscriptionService.subscribe(principal.memberId(), request.billingKeyId())));
	}

	@Operation(summary = "내 구독 상태 조회")
	@ApiResponse(responseCode = "200", description = "조회 성공")
	@ApiResponse(responseCode = "400", description = "구독 내역이 없음",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@GetMapping("/me")
	public ResponseEntity<SubscriptionResponse> getCurrent(@AuthenticationPrincipal MemberPrincipal principal) {
		return ResponseEntity.ok(SubscriptionResponse.of(subscriptionService.getCurrent(principal.memberId())));
	}
}
