package com.alrdream.domain.coupon.domain;

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
import org.hibernate.annotations.UuidGenerator;

/**
 * [04_milestone.md] Phase 19 — 이벤트로 외부에 공개하는 쿠폰 코드. 코드는 관리자가 직접 문자열을 정한다
 * (마케팅이 미리 공지하는 코드라서 자동 생성이 아니다).
 */
@Getter
@Entity
@Table(name = "coupons")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Coupon extends BaseEntity {

	@Id
	@UuidGenerator
	private UUID id;

	@Column(nullable = false, unique = true)
	private String code;

	@Column(name = "benefit_days", nullable = false)
	private int benefitDays;

	/** null이면 무제한. */
	@Column(name = "max_redemptions")
	private Integer maxRedemptions;

	@Column(name = "redemption_count", nullable = false)
	private int redemptionCount;

	/** null이면 코드 자체의 사용 기한 없음. */
	@Column(name = "expires_at")
	private OffsetDateTime expiresAt;

	@Column(nullable = false)
	private boolean active;

	private Coupon(String code, int benefitDays, Integer maxRedemptions, OffsetDateTime expiresAt) {
		this.code = code;
		this.benefitDays = benefitDays;
		this.maxRedemptions = maxRedemptions;
		this.redemptionCount = 0;
		this.expiresAt = expiresAt;
		this.active = true;
	}

	public static Coupon create(String code, int benefitDays, Integer maxRedemptions, OffsetDateTime expiresAt) {
		return new Coupon(code, benefitDays, maxRedemptions, expiresAt);
	}

	public void deactivate() {
		this.active = false;
	}

	/** 이 쿠폰을 지금 사용할 수 있는지 — 사용 여부(중복 사용) 검사는 별도(CouponRedemption unique 제약 + 서비스 조회). */
	public boolean isRedeemableAt(OffsetDateTime now) {
		if (!active) {
			return false;
		}
		if (expiresAt != null && !now.isBefore(expiresAt)) {
			return false;
		}
		return maxRedemptions == null || redemptionCount < maxRedemptions;
	}

	public void incrementRedemptionCount() {
		this.redemptionCount++;
	}
}
