package com.alrdream.domain.planning.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreatePlanningVersionRequest(
		@Schema(description = "기획 생성/수정에 사용할 설문 응답 ID (PLANNING_HAS_IDEA 또는 PLANNING_EXPLORING)")
				@NotNull UUID surveyResponseId) {
}
