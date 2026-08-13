package com.alrdream.domain.coupon.api;

import com.alrdream.domain.coupon.api.dto.RedeemCouponRequest;
import com.alrdream.domain.coupon.api.dto.RedeemCouponResponse;
import com.alrdream.domain.coupon.application.CouponService;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Coupon", description = "쿠폰 코드 등록 — [04_milestone.md] Phase 19")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/coupons")
public class CouponController {

	private final CouponService couponService;

	public CouponController(CouponService couponService) {
		this.couponService = couponService;
	}

	@Operation(summary = "쿠폰 코드 등록", description = "Free면 신규로 Pro를 지급받고, 이미 Pro라면 보장 기간이 쿠폰 일수만큼 늘어난다.")
	@ApiResponse(responseCode = "200", description = "등록 성공")
	@ApiResponse(responseCode = "400", description = "존재하지 않는 코드/사용할 수 없는 쿠폰/이미 사용한 쿠폰",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@PostMapping("/redeem")
	public ResponseEntity<RedeemCouponResponse> redeem(
			@AuthenticationPrincipal MemberPrincipal principal, @Valid @RequestBody RedeemCouponRequest request) {
		CouponService.RedeemResult result = couponService.redeem(principal.memberId(), request.code());
		return ResponseEntity.ok(new RedeemCouponResponse(result.benefitDays(), result.proExpiresAt()));
	}
}
