package com.alrdream.domain.coupon.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;

public record RedeemCouponResponse(
		@Schema(description = "지급된 일수") int benefitDays,
		@Schema(description = "적용 후 Pro 보장 만료 시각") OffsetDateTime proExpiresAt) {
}
