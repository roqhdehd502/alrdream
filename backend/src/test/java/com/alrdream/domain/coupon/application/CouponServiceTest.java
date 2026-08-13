package com.alrdream.domain.coupon.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.alrdream.domain.coupon.domain.Coupon;
import com.alrdream.domain.coupon.domain.CouponRedemptionRepository;
import com.alrdream.domain.coupon.domain.CouponRepository;
import com.alrdream.domain.member.domain.Member;
import com.alrdream.domain.member.domain.MemberPlan;
import com.alrdream.domain.member.domain.MemberRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * {@link CouponService} 단위 테스트 — 동시성 방지용 advisory lock 네이티브 쿼리는 Mockito로 무해화하고
 * (Phase 21에서 추가한 락 자체의 실제 직렬화 동작은 DB가 필요해 여기서 검증하지 않는다), 상환 검증 로직만 본다.
 */
@ExtendWith(MockitoExtension.class)
class CouponServiceTest {

	@Mock
	private CouponRepository couponRepository;
	@Mock
	private CouponRedemptionRepository couponRedemptionRepository;
	@Mock
	private MemberRepository memberRepository;
	@Mock
	private EntityManager entityManager;
	@Mock
	private Query lockQuery;

	private CouponService couponService;
	private UUID userId;

	@BeforeEach
	void setUp() {
		couponService = new CouponService(couponRepository, couponRedemptionRepository, memberRepository);
		ReflectionTestUtils.setField(couponService, "entityManager", entityManager);
		userId = UUID.randomUUID();

		lenient().when(entityManager.createNativeQuery(anyString())).thenReturn(lockQuery);
		lenient().when(lockQuery.setParameter(anyString(), any())).thenReturn(lockQuery);
		lenient().when(lockQuery.getSingleResult()).thenReturn(null);
	}

	private Coupon couponWith(int benefitDays, Integer maxRedemptions) {
		Coupon coupon = Coupon.create("CODE1", benefitDays, maxRedemptions, null);
		ReflectionTestUtils.setField(coupon, "id", UUID.randomUUID());
		return coupon;
	}

	@Test
	void redeem_존재하지_않는_코드면_예외() {
		when(couponRepository.findByCode("NOPE")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> couponService.redeem(userId, "NOPE"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("존재하지 않는");
	}

	@Test
	void redeem_비활성화된_쿠폰은_거부() {
		Coupon coupon = couponWith(7, null);
		coupon.deactivate();
		when(couponRepository.findByCode("CODE1")).thenReturn(Optional.of(coupon));

		assertThatThrownBy(() -> couponService.redeem(userId, "CODE1"))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void redeem_이미_사용한_쿠폰은_거부() {
		Coupon coupon = couponWith(7, null);
		when(couponRepository.findByCode("CODE1")).thenReturn(Optional.of(coupon));
		when(couponRedemptionRepository.existsByCouponIdAndUserId(coupon.getId(), userId)).thenReturn(true);

		assertThatThrownBy(() -> couponService.redeem(userId, "CODE1"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("이미 사용");
	}

	@Test
	void redeem_maxRedemptions에_도달한_쿠폰은_거부() {
		Coupon coupon = couponWith(7, 1);
		coupon.incrementRedemptionCount(); // 이미 1/1 소진
		when(couponRepository.findByCode("CODE1")).thenReturn(Optional.of(coupon));

		assertThatThrownBy(() -> couponService.redeem(userId, "CODE1"))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void redeem_성공하면_Pro를_지급하고_상환_기록을_남김() {
		Coupon coupon = couponWith(10, null);
		Member member = Member.createLocal("user@example.com", "hashed");
		when(couponRepository.findByCode("CODE1")).thenReturn(Optional.of(coupon));
		when(couponRedemptionRepository.existsByCouponIdAndUserId(coupon.getId(), userId)).thenReturn(false);
		when(memberRepository.findById(userId)).thenReturn(Optional.of(member));

		CouponService.RedeemResult result = couponService.redeem(userId, "CODE1");

		assertThat(result.benefitDays()).isEqualTo(10);
		assertThat(member.getPlan()).isEqualTo(MemberPlan.PRO);
		assertThat(coupon.getRedemptionCount()).isEqualTo(1);
		org.mockito.Mockito.verify(couponRedemptionRepository).save(any());
	}

	@Test
	void redeem_존재하지_않는_회원이면_예외() {
		Coupon coupon = couponWith(10, null);
		when(couponRepository.findByCode("CODE1")).thenReturn(Optional.of(coupon));
		when(couponRedemptionRepository.existsByCouponIdAndUserId(coupon.getId(), userId)).thenReturn(false);
		when(memberRepository.findById(userId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> couponService.redeem(userId, "CODE1"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("존재하지 않는 회원");
	}
}
