package com.alrdream.domain.ai.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;

public record UpdateFreeTierLimitRequest(
		@Schema(description = "새 FREE 플랜 월별 생성 횟수 한도") @Min(0) int monthlyLimit) {
}
