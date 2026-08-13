package com.alrdream.domain.subscription.api;

import com.alrdream.domain.subscription.api.dto.SetPromotionRequest;
import com.alrdream.domain.subscription.api.dto.SubscriptionPricingAdminResponse;
import com.alrdream.domain.subscription.api.dto.UpdateBasePriceRequest;
import com.alrdream.domain.subscription.application.SubscriptionPricingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Subscription Pricing (Admin)", description = "Pro 구독 월 요금/프로모션 조회·관리 — Admin 전용 [01] 13번, [03] §2-1")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/admin/subscriptions/pricing")
public class SubscriptionPricingAdminController {

	private final SubscriptionPricingService subscriptionPricingService;

	public SubscriptionPricingAdminController(SubscriptionPricingService subscriptionPricingService) {
		this.subscriptionPricingService = subscriptionPricingService;
	}

	@Operation(summary = "Pro 구독 가격/프로모션 조회")
	@ApiResponse(responseCode = "200", description = "조회 성공")
	@GetMapping
	public ResponseEntity<SubscriptionPricingAdminResponse> get() {
		return ResponseEntity.ok(SubscriptionPricingAdminResponse.of(subscriptionPricingService.get()));
	}

	@Operation(summary = "기본 월 요금 변경",
			description = "이후 새로 청구되는 결제(신규 구독/기존 구독 다음 달 갱신 모두)부터 적용된다. 이미 예약된 다음 결제 "
					+ "건의 금액은 소급 변경되지 않는다(다음 갱신 재예약 시점부터 반영).")
	@ApiResponse(responseCode = "200", description = "변경 성공")
	@PutMapping("/base-price")
	public ResponseEntity<SubscriptionPricingAdminResponse> updateBasePrice(@Valid @RequestBody UpdateBasePriceRequest request) {
		return ResponseEntity.ok(
				SubscriptionPricingAdminResponse.of(subscriptionPricingService.updateBasePrice(request.basePriceKrw())));
	}

	@Operation(summary = "프로모션 설정",
			description = "지정한 기간 동안 promoPriceKrw가 유효가로 쓰인다. 기존 프로모션이 있으면 덮어쓴다.")
	@ApiResponse(responseCode = "200", description = "설정 성공")
	@ApiResponse(responseCode = "400", description = "종료 시각이 시작 시각보다 이르거나 같음")
	@PutMapping("/promotion")
	public ResponseEntity<SubscriptionPricingAdminResponse> setPromotion(@Valid @RequestBody SetPromotionRequest request) {
		return ResponseEntity.ok(SubscriptionPricingAdminResponse.of(subscriptionPricingService.setPromotion(
				request.promoPriceKrw(), request.promoStartsAt(), request.promoEndsAt())));
	}

	@Operation(summary = "프로모션 조기 종료/해제")
	@ApiResponse(responseCode = "200", description = "해제 성공")
	@DeleteMapping("/promotion")
	public ResponseEntity<SubscriptionPricingAdminResponse> clearPromotion() {
		return ResponseEntity.ok(SubscriptionPricingAdminResponse.of(subscriptionPricingService.clearPromotion()));
	}
}
