package com.alrdream.domain.survey.api.dto;

import com.alrdream.domain.survey.domain.SurveyKey;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record SubmitSurveyResponseRequest(
		@Schema(description = "제출 대상 설문 종류 — 항상 최신 발행 버전을 기준으로 검증한다") @NotNull SurveyKey surveyKey,
		@Schema(description = "답변 목록") @NotEmpty @Valid List<SurveyAnswerDto> answers) {
}
