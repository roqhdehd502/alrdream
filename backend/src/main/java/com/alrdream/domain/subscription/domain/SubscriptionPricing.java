package com.alrdream.domain.subscription.domain;

import com.alrdream.global.jpa.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * [03] §2-1 {@code subscription_pricing} — Pro 구독 월 요금을 Admin이 조정할 수 있도록 담은 단일 행 테이블
 * ({@code free_tier_settings}와 동일한 패턴). 프로모션 기간에는 {@code promoPriceKrw}가 유효가로 쓰인다.
 * 가입 시점 가격을 구독별로 잠그지 않는다 — 신규 구독/기존 구독 갱신 모두 청구 시점의 {@link #effectivePriceKrw}를
 * 그대로 쓴다(04_milestone.md 설계 결정 참고).
 */
@Getter
@Entity
@Table(name = "subscription_pricing")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SubscriptionPricing extends BaseEntity {

	@Id
	private UUID id;

	@Column(name = "base_price_krw", nullable = false)
	private long basePriceKrw;

	@Column(name = "promo_price_krw")
	private Long promoPriceKrw;

	@Column(name = "promo_starts_at")
	private OffsetDateTime promoStartsAt;

	@Column(name = "promo_ends_at")
	private OffsetDateTime promoEndsAt;

	public void changeBasePrice(long basePriceKrw) {
		this.basePriceKrw = basePriceKrw;
	}

	public void setPromotion(long promoPriceKrw, OffsetDateTime promoStartsAt, OffsetDateTime promoEndsAt) {
		if (!promoEndsAt.isAfter(promoStartsAt)) {
			throw new IllegalArgumentException("프로모션 종료 시각은 시작 시각보다 이후여야 합니다.");
		}
		this.promoPriceKrw = promoPriceKrw;
		this.promoStartsAt = promoStartsAt;
		this.promoEndsAt = promoEndsAt;
	}

	public void clearPromotion() {
		this.promoPriceKrw = null;
		this.promoStartsAt = null;
		this.promoEndsAt = null;
	}

	public boolean isPromoActiveAt(OffsetDateTime now) {
		return promoPriceKrw != null && !now.isBefore(promoStartsAt) && now.isBefore(promoEndsAt);
	}

	public long effectivePriceKrw(OffsetDateTime now) {
		return isPromoActiveAt(now) ? promoPriceKrw : basePriceKrw;
	}
}
