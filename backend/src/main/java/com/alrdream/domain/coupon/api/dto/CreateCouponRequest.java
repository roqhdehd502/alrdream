package com.alrdream.domain.coupon.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.time.OffsetDateTime;

public record CreateCouponRequest(
		@Schema(description = "쿠폰 코드(직접 지정)") @NotBlank String code,
		@Schema(description = "지급 일수") @Min(1) int benefitDays,
		@Schema(description = "최대 사용 횟수, 생략하면 무제한") Integer maxRedemptions,
		@Schema(description = "코드 사용 기한, 생략하면 무기한") OffsetDateTime expiresAt) {
}
