package com.alrdream.infrastructure.ai;

/**
 * [03] §4-3 — {@link AiClient}에 넘길 구조화 생성 요청. {@code inputSchemaJson}은 Claude Tool Use의
 * {@code input_schema}로 그대로 전달되는 JSON Schema 문자열이다.
 */
public record AiGenerationRequest(
		String systemPrompt,
		String userPrompt,
		String toolName,
		String toolDescription,
		String inputSchemaJson) {
}
