package com.alrdream.domain.survey.api.dto;

import com.alrdream.domain.survey.domain.QuestionType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record QuestionDto(
		@Schema(description = "문항 ID", example = "Q1") @NotBlank String id,
		@Schema(description = "AI 프롬프트 변수명", example = "idea_summary") @NotBlank String promptKey,
		@Schema(description = "문항 타입") @NotNull QuestionType type,
		@Schema(description = "질문 텍스트") @NotBlank String question,
		@Schema(description = "필수 응답 여부") boolean required,
		@Schema(description = "선택형(SINGLE_CHOICE/MULTI_CHOICE) 문항의 보기 — 그 외 타입은 비워둔다") @Valid List<OptionDto> options,
		@Schema(description = "\"잘 모르겠어요\" 응답 허용 여부") boolean allowUnknown) {

	public record OptionDto(
			@Schema(description = "보기 값") @NotBlank String key,
			@Schema(description = "보기 라벨") @NotBlank String label) {
	}
}
