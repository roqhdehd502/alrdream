package com.alrdream.domain.ai.api.dto;

import com.alrdream.domain.ai.domain.FreeTierSetting;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;

public record FreeTierLimitResponse(
		@Schema(description = "FREE 플랜 월별 생성 횟수 한도") int monthlyLimit,
		@Schema(description = "마지막 수정 시각") OffsetDateTime updatedAt) {

	public static FreeTierLimitResponse from(FreeTierSetting setting) {
		return new FreeTierLimitResponse(setting.getMonthlyLimit(), setting.getUpdatedAt());
	}
}
