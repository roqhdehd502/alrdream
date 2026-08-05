package com.alrdream.domain.survey.api.dto;

import com.alrdream.domain.survey.domain.QuestionType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * [02] §4 설문 응답 스키마의 answer 1건. {@code values}는 문서의 단수형 {@code value}를 일반화한 것 —
 * SINGLE_CHOICE/SHORT_TEXT/LONG_TEXT/SCALE은 원소 1개, MULTI_CHOICE는 1개 이상을 담는다.
 */
public record SurveyAnswerDto(
		@Schema(description = "문항 ID", example = "Q1") @NotBlank String questionId,
		@Schema(description = "문항 타입 (설문 정의와 일치해야 함)") @NotNull QuestionType type,
		@Schema(description = "답변 값 목록 — SINGLE_CHOICE/SCALE/TEXT는 1개, MULTI_CHOICE는 1개 이상") List<String> values,
		@Schema(description = "\"잘 모르겠어요\" 응답 여부 — 해당 문항이 allowUnknown=true일 때만 허용") boolean isUnknown) {
}
