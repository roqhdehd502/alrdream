package com.alrdream.domain.coupon.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

/**
 * [04_milestone.md] Phase 19 {@code coupon_redemptions} — 사용자별 쿠폰 사용 이력 1건.
 * {@code UNIQUE(coupon_id, user_id)}가 동일 유저의 동일 코드 중복 사용을 DB 레벨에서 막는다.
 * {@code updated_at}이 없어 {@link com.alrdream.global.jpa.BaseEntity}는 쓰지 않는다
 * ({@code payment_history}와 동일한 이유).
 */
@Getter
@Entity
@Table(name = "coupon_redemptions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CouponRedemption {

	@Id
	@UuidGenerator
	private UUID id;

	@Column(name = "coupon_id", nullable = false)
	private UUID couponId;

	@Column(name = "user_id", nullable = false)
	private UUID userId;

	@Column(name = "redeemed_at", nullable = false)
	private OffsetDateTime redeemedAt;

	@Column(name = "granted_until", nullable = false)
	private OffsetDateTime grantedUntil;

	private CouponRedemption(UUID couponId, UUID userId, OffsetDateTime grantedUntil) {
		this.couponId = couponId;
		this.userId = userId;
		this.redeemedAt = OffsetDateTime.now();
		this.grantedUntil = grantedUntil;
	}

	public static CouponRedemption create(UUID couponId, UUID userId, OffsetDateTime grantedUntil) {
		return new CouponRedemption(couponId, userId, grantedUntil);
	}
}
