package com.alrdream.domain.coupon.application;

import com.alrdream.domain.coupon.api.dto.CouponRedemptionAdminResponse;
import com.alrdream.domain.coupon.domain.Coupon;
import com.alrdream.domain.coupon.domain.CouponRedemption;
import com.alrdream.domain.coupon.domain.CouponRedemptionRepository;
import com.alrdream.domain.coupon.domain.CouponRepository;
import com.alrdream.domain.member.domain.Member;
import com.alrdream.domain.member.domain.MemberRepository;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** [03] §2-1 Admin의 "쿠폰" 관리 — 코드 생성/목록/비활성화, 사용 현황 조회. */
@Service
@Transactional(readOnly = true)
public class CouponAdminService {

	private final CouponRepository couponRepository;
	private final CouponRedemptionRepository couponRedemptionRepository;
	private final MemberRepository memberRepository;

	public CouponAdminService(
			CouponRepository couponRepository,
			CouponRedemptionRepository couponRedemptionRepository,
			MemberRepository memberRepository) {
		this.couponRepository = couponRepository;
		this.couponRedemptionRepository = couponRedemptionRepository;
		this.memberRepository = memberRepository;
	}

	public Page<Coupon> list(Pageable pageable) {
		return couponRepository.findAll(pageable);
	}

	@Transactional
	public Coupon create(String code, int benefitDays, Integer maxRedemptions, OffsetDateTime expiresAt) {
		if (couponRepository.existsByCode(code)) {
			throw new IllegalArgumentException("이미 존재하는 쿠폰 코드입니다.");
		}
		return couponRepository.save(Coupon.create(code, benefitDays, maxRedemptions, expiresAt));
	}

	@Transactional
	public Coupon deactivate(UUID couponId) {
		Coupon coupon = couponRepository.findById(couponId)
				.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 쿠폰입니다."));
		coupon.deactivate();
		return coupon;
	}

	public Page<CouponRedemptionAdminResponse> listRedemptions(Pageable pageable) {
		Page<CouponRedemption> page = couponRedemptionRepository.findAll(pageable);

		Map<UUID, String> codeByCouponId = couponRepository
				.findAllById(page.getContent().stream().map(CouponRedemption::getCouponId).distinct().toList())
				.stream()
				.collect(Collectors.toMap(Coupon::getId, Coupon::getCode));

		Map<UUID, String> emailByUserId = memberRepository
				.findAllById(page.getContent().stream().map(CouponRedemption::getUserId).distinct().toList())
				.stream()
				.collect(Collectors.toMap(Member::getId, Member::getEmail));

		return page.map(redemption -> CouponRedemptionAdminResponse.of(
				redemption, codeByCouponId.get(redemption.getCouponId()), emailByUserId.get(redemption.getUserId())));
	}
}
