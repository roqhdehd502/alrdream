package com.alrdream.domain.prompt.api.dto;

import com.alrdream.domain.ai.domain.AiTargetType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** 버전 번호는 클라이언트가 지정하지 않는다 — 서버가 해당 promptType의 최신 버전 + 1로 자동 계산한다(발행). */
public record PublishPromptTemplateRequest(
		@Schema(description = "프롬프트 대상 (기획/분석/설계)") @NotNull AiTargetType promptType,
		@Schema(description = "Claude Tool Use 도구 이름", example = "emit_planning_document") @NotBlank String toolName,
		@Schema(description = "Claude Tool Use 도구 설명") @NotBlank String toolDescription,
		@Schema(description = "시스템 프롬프트") @NotBlank String systemPrompt,
		@Schema(description = "Claude Tool Use input_schema로 전달될 JSON Schema 문자열") @NotBlank String schemaJson) {
}
