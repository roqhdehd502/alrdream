package com.alrdream.domain.subscription.api.dto;

import com.alrdream.domain.subscription.domain.SubscriptionPricing;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;

/** 구독 상품 소개 화면에 쓰는 회원용 가격 조회 응답 — Admin 응답과 달리 내부 상세(promoStartsAt 등)는 뺀다. */
public record PricingResponse(
		@Schema(description = "기본 월 요금(원)") long basePriceKrw,
		@Schema(description = "프로모션 진행 중이면 청구 금액(원), 아니면 null") Long promoPriceKrw,
		@Schema(description = "프로모션 종료 시각(진행 중일 때만)") OffsetDateTime promoEndsAt,
		@Schema(description = "현재 시점 청구 금액(원)") long effectivePriceKrw) {

	public static PricingResponse of(SubscriptionPricing pricing) {
		OffsetDateTime now = OffsetDateTime.now();
		boolean active = pricing.isPromoActiveAt(now);
		return new PricingResponse(
				pricing.getBasePriceKrw(),
				active ? pricing.getPromoPriceKrw() : null,
				active ? pricing.getPromoEndsAt() : null,
				pricing.effectivePriceKrw(now));
	}
}
