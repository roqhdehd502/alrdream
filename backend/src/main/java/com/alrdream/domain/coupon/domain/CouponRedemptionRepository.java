package com.alrdream.domain.coupon.domain;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CouponRedemptionRepository extends JpaRepository<CouponRedemption, UUID> {

	boolean existsByCouponIdAndUserId(UUID couponId, UUID userId);
}
