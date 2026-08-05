package com.alrdream.domain.survey.api.dto;

import com.alrdream.domain.survey.domain.SurveyKey;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/** 버전 번호는 클라이언트가 지정하지 않는다 — 서버가 해당 surveyKey의 최신 버전 + 1로 자동 계산한다(발행). */
public record PublishSurveyDefinitionRequest(
		@Schema(description = "설문 종류") @NotNull SurveyKey surveyKey,
		@Schema(description = "설문 제목", example = "사업 아이템 기획 설문") @NotBlank @Size(max = 255) String title,
		@Schema(description = "문항 목록") @NotEmpty @Valid List<QuestionDto> questions) {
}
