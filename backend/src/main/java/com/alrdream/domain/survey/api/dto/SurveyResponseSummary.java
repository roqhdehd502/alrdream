package com.alrdream.domain.survey.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.UUID;

public record SurveyResponseSummary(
		@Schema(description = "응답 ID") UUID id,
		@Schema(description = "설문 정의 ID") UUID surveyDefinitionId,
		@Schema(description = "제출 시각") OffsetDateTime submittedAt) {
}
