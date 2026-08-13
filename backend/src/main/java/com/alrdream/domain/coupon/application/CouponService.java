package com.alrdream.domain.coupon.application;

import com.alrdream.domain.coupon.domain.Coupon;
import com.alrdream.domain.coupon.domain.CouponRedemption;
import com.alrdream.domain.coupon.domain.CouponRedemptionRepository;
import com.alrdream.domain.coupon.domain.CouponRepository;
import com.alrdream.domain.member.domain.Member;
import com.alrdream.domain.member.domain.MemberRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
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

	@PersistenceContext
	private EntityManager entityManager;

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
		// 같은 쿠폰에 대한 동시 사용 요청이 각자 redemptionCount를 읽어 한도 검사를 모두 통과하면
		// max_redemptions를 초과해 지급될 수 있다 — 조회 전에 쿠폰 코드 단위로 직렬화해, 락을 기다렸다 통과한
		// 요청은 반드시 직전 요청이 커밋한 최신 redemptionCount를 읽도록 한다(UsageQuotaService와 동일 패턴).
		entityManager.createNativeQuery("SELECT pg_advisory_xact_lock(hashtext(:key)::bigint)")
				.setParameter("key", "coupon:" + code)
				.getSingleResult();

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
