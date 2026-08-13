package com.alrdream.domain.coupon.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record RedeemCouponRequest(@Schema(description = "쿠폰 코드") @NotBlank String code) {
}
