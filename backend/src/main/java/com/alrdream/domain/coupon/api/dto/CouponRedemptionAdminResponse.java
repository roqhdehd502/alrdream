package com.alrdream.domain.coupon.api.dto;

import com.alrdream.domain.coupon.domain.CouponRedemption;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.UUID;

public record CouponRedemptionAdminResponse(
		@Schema(description = "사용 이력 ID") UUID id,
		@Schema(description = "쿠폰 코드") String couponCode,
		@Schema(description = "사용자 ID") UUID userId,
		@Schema(description = "사용자 이메일") String userEmail,
		@Schema(description = "사용 시각") OffsetDateTime redeemedAt,
		@Schema(description = "적용 후 Pro 보장 만료 시각") OffsetDateTime grantedUntil) {

	public static CouponRedemptionAdminResponse of(CouponRedemption redemption, String couponCode, String userEmail) {
		return new CouponRedemptionAdminResponse(
				redemption.getId(), couponCode, redemption.getUserId(), userEmail,
				redemption.getRedeemedAt(), redemption.getGrantedUntil());
	}
}
