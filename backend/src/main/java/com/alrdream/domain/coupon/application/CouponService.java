package com.alrdream.domain.coupon.application;

import com.alrdream.domain.coupon.domain.Coupon;
import com.alrdream.domain.coupon.domain.CouponRedemption;
import com.alrdream.domain.coupon.domain.CouponRedemptionRepository;
import com.alrdream.domain.coupon.domain.CouponRepository;
import com.alrdream.domain.member.domain.Member;
import com.alrdream.domain.member.domain.MemberRepository;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** [04_milestone.md] Phase 19 — 회원이 직접 쿠폰 코드를 등록해 Pro를 지급받는다. */
@Service
@Transactional(readOnly = true)
public class CouponService {

	private final CouponRepository couponRepository;
	private final CouponRedemptionRepository couponRedemptionRepository;
	private final MemberRepository memberRepository;

	public CouponService(
			CouponRepository couponRepository,
			CouponRedemptionRepository couponRedemptionRepository,
			MemberRepository memberRepository) {
		this.couponRepository = couponRepository;
		this.couponRedemptionRepository = couponRedemptionRepository;
		this.memberRepository = memberRepository;
	}

	@Transactional
	public RedeemResult redeem(UUID userId, String code) {
		Coupon coupon = couponRepository.findByCode(code)
				.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 쿠폰 코드입니다."));
		if (!coupon.isRedeemableAt(OffsetDateTime.now())) {
			throw new IllegalArgumentException("사용할 수 없는 쿠폰입니다(만료되었거나 비활성화되었거나 사용 한도를 초과했습니다).");
		}
		if (couponRedemptionRepository.existsByCouponIdAndUserId(coupon.getId(), userId)) {
			throw new IllegalArgumentException("이미 사용한 쿠폰입니다.");
		}

		Member member = memberRepository.findById(userId)
				.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));
		member.extendProUntil(coupon.getBenefitDays());
		coupon.incrementRedemptionCount();

		couponRedemptionRepository.save(CouponRedemption.create(coupon.getId(), userId, member.getProExpiresAt()));
		return new RedeemResult(coupon.getBenefitDays(), member.getProExpiresAt());
	}

	public record RedeemResult(int benefitDays, OffsetDateTime proExpiresAt) {
	}
}
