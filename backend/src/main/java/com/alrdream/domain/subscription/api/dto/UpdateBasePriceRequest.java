package com.alrdream.domain.subscription.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;

public record UpdateBasePriceRequest(
		@Schema(description = "새 Pro 구독 월 요금(원)") @Min(0) long basePriceKrw) {
}
