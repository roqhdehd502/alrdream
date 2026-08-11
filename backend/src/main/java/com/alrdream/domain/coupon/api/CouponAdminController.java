package com.alrdream.domain.coupon.api;

import com.alrdream.domain.coupon.api.dto.CouponRedemptionAdminResponse;
import com.alrdream.domain.coupon.api.dto.CouponResponse;
import com.alrdream.domain.coupon.api.dto.CreateCouponRequest;
import com.alrdream.domain.coupon.application.CouponAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PagedModel;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Coupon (Admin)", description = "쿠폰 코드 관리 + 사용 현황 조회 — Admin 전용 [04_milestone.md] Phase 19")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/admin/coupons")
public class CouponAdminController {

	private final CouponAdminService couponAdminService;

	public CouponAdminController(CouponAdminService couponAdminService) {
		this.couponAdminService = couponAdminService;
	}

	@Operation(summary = "쿠폰 코드 목록 조회")
	@ApiResponse(responseCode = "200", description = "조회 성공")
	@GetMapping
	public ResponseEntity<PagedModel<CouponResponse>> list(
			@ParameterObject
			@PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
		Page<CouponResponse> page = couponAdminService.list(pageable).map(CouponResponse::of);
		return ResponseEntity.ok(new PagedModel<>(page));
	}

	@Operation(summary = "쿠폰 코드 생성")
	@ApiResponse(responseCode = "200", description = "생성 성공")
	@ApiResponse(responseCode = "400", description = "이미 존재하는 코드")
	@PostMapping
	public ResponseEntity<CouponResponse> create(@Valid @RequestBody CreateCouponRequest request) {
		return ResponseEntity.ok(CouponResponse.of(couponAdminService.create(
				request.code(), request.benefitDays(), request.maxRedemptions(), request.expiresAt())));
	}

	@Operation(summary = "쿠폰 코드 비활성화", description = "이미 사용된 이력은 남고, 이후 신규 사용만 막는다.")
	@ApiResponse(responseCode = "200", description = "비활성화 성공")
	@PatchMapping("/{couponId}/deactivate")
	public ResponseEntity<CouponResponse> deactivate(@PathVariable UUID couponId) {
		return ResponseEntity.ok(CouponResponse.of(couponAdminService.deactivate(couponId)));
	}

	@Operation(summary = "유저 쿠폰 사용 현황 조회")
	@ApiResponse(responseCode = "200", description = "조회 성공")
	@GetMapping("/redemptions")
	public ResponseEntity<PagedModel<CouponRedemptionAdminResponse>> redemptions(
			@ParameterObject
			@PageableDefault(size = 20, sort = "redeemedAt", direction = Sort.Direction.DESC) Pageable pageable) {
		return ResponseEntity.ok(new PagedModel<>(couponAdminService.listRedemptions(pageable)));
	}
}
