package com.alrdream.domain.prompt.api.dto;

import com.alrdream.domain.ai.domain.AiTargetType;
import com.alrdream.domain.prompt.domain.PromptTemplate;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PromptTemplateResponse(
		@Schema(description = "프롬프트 템플릿 ID") UUID id,
		@Schema(description = "프롬프트 대상") AiTargetType promptType,
		@Schema(description = "버전") int version,
		@Schema(description = "Claude Tool Use 도구 이름") String toolName,
		@Schema(description = "Claude Tool Use 도구 설명") String toolDescription,
		@Schema(description = "시스템 프롬프트") String systemPrompt,
		@Schema(description = "JSON Schema 문자열") String schemaJson,
		@Schema(description = "발행 시각") OffsetDateTime createdAt) {

	public static PromptTemplateResponse from(PromptTemplate template) {
		return new PromptTemplateResponse(
				template.getId(),
				template.getPromptType(),
				template.getVersion(),
				template.getToolName(),
				template.getToolDescription(),
				template.getSystemPrompt(),
				template.getSchemaJson(),
				template.getCreatedAt());
	}
}
