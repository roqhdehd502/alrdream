package com.alrdream.domain.coupon.api.dto;

import com.alrdream.domain.coupon.domain.Coupon;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.UUID;

public record CouponResponse(
		@Schema(description = "쿠폰 ID") UUID id,
		@Schema(description = "쿠폰 코드") String code,
		@Schema(description = "지급 일수") int benefitDays,
		@Schema(description = "최대 사용 횟수, null이면 무제한") Integer maxRedemptions,
		@Schema(description = "누적 사용 횟수") int redemptionCount,
		@Schema(description = "코드 사용 기한, null이면 무기한") OffsetDateTime expiresAt,
		@Schema(description = "활성 여부") boolean active,
		@Schema(description = "생성 시각") OffsetDateTime createdAt) {

	public static CouponResponse of(Coupon coupon) {
		return new CouponResponse(
				coupon.getId(), coupon.getCode(), coupon.getBenefitDays(), coupon.getMaxRedemptions(),
				coupon.getRedemptionCount(), coupon.getExpiresAt(), coupon.isActive(), coupon.getCreatedAt());
	}
}
