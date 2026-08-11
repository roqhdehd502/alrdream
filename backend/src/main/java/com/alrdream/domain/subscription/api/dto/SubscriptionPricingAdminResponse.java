package com.alrdream.domain.subscription.api.dto;

import com.alrdream.domain.subscription.domain.SubscriptionPricing;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;

public record SubscriptionPricingAdminResponse(
		@Schema(description = "기본 월 요금(원)") long basePriceKrw,
		@Schema(description = "프로모션 중 청구 금액(원), 프로모션 없으면 null") Long promoPriceKrw,
		@Schema(description = "프로모션 시작 시각") OffsetDateTime promoStartsAt,
		@Schema(description = "프로모션 종료 시각") OffsetDateTime promoEndsAt,
		@Schema(description = "현재 프로모션 진행 중 여부") boolean promoActive,
		@Schema(description = "현재 시점 청구 금액(원) — 프로모션 진행 중이면 promoPriceKrw, 아니면 basePriceKrw") long effectivePriceKrw,
		@Schema(description = "마지막 수정 시각") OffsetDateTime updatedAt) {

	public static SubscriptionPricingAdminResponse of(SubscriptionPricing pricing) {
		OffsetDateTime now = OffsetDateTime.now();
		return new SubscriptionPricingAdminResponse(
				pricing.getBasePriceKrw(), pricing.getPromoPriceKrw(), pricing.getPromoStartsAt(),
				pricing.getPromoEndsAt(), pricing.isPromoActiveAt(now), pricing.effectivePriceKrw(now),
				pricing.getUpdatedAt());
	}
}
