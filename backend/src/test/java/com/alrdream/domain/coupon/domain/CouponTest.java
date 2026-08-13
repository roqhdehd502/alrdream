package com.alrdream.domain.coupon.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

class CouponTest {

	@Test
	void isRedeemableAt_기본값은_사용_가능() {
		Coupon coupon = Coupon.create("CODE1", 7, null, null);

		assertThat(coupon.isRedeemableAt(OffsetDateTime.now())).isTrue();
	}

	@Test
	void isRedeemableAt_비활성화되면_사용_불가() {
		Coupon coupon = Coupon.create("CODE1", 7, null, null);
		coupon.deactivate();

		assertThat(coupon.isRedeemableAt(OffsetDateTime.now())).isFalse();
	}

	@Test
	void isRedeemableAt_사용_기한이_지나면_사용_불가() {
		Coupon coupon = Coupon.create("CODE1", 7, null, OffsetDateTime.now().minusDays(1));

		assertThat(coupon.isRedeemableAt(OffsetDateTime.now())).isFalse();
	}

	@Test
	void isRedeemableAt_사용_기한_이전이면_사용_가능() {
		Coupon coupon = Coupon.create("CODE1", 7, null, OffsetDateTime.now().plusDays(1));

		assertThat(coupon.isRedeemableAt(OffsetDateTime.now())).isTrue();
	}

	@Test
	void isRedeemableAt_maxRedemptions에_도달하면_사용_불가() {
		Coupon coupon = Coupon.create("CODE1", 7, 2, null);
		coupon.incrementRedemptionCount();
		coupon.incrementRedemptionCount();

		assertThat(coupon.isRedeemableAt(OffsetDateTime.now())).isFalse();
	}

	@Test
	void isRedeemableAt_maxRedemptions_직전까지는_사용_가능() {
		Coupon coupon = Coupon.create("CODE1", 7, 2, null);
		coupon.incrementRedemptionCount();

		assertThat(coupon.isRedeemableAt(OffsetDateTime.now())).isTrue();
	}

	@Test
	void isRedeemableAt_maxRedemptions_null이면_무제한() {
		Coupon coupon = Coupon.create("CODE1", 7, null, null);
		for (int i = 0; i < 1000; i++) {
			coupon.incrementRedemptionCount();
		}

		assertThat(coupon.isRedeemableAt(OffsetDateTime.now())).isTrue();
	}
}
