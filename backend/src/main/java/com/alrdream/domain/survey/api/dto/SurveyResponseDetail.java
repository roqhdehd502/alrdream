package com.alrdream.domain.survey.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record SurveyResponseDetail(
		@Schema(description = "응답 ID") UUID id,
		@Schema(description = "설문 정의 ID") UUID surveyDefinitionId,
		@Schema(description = "제출 시각") OffsetDateTime submittedAt,
		@Schema(description = "답변 목록 (복호화됨)") List<SurveyAnswerDto> answers) {
}
