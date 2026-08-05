package com.alrdream.infrastructure.ai;

/**
 * [03] §4-3 — LLM 연동 인터페이스. 1차 구현체는 Claude API({@link ClaudeAiClient})지만, 추후 Provider를
 * 추가/교체할 수 있도록 인터페이스로 분리한다.
 */
public interface AiClient {

	/**
	 * 자유 텍스트가 아니라, {@code inputSchemaJson}(JSON Schema)을 따르는 구조화된 JSON 문자열을 강제로 반환받는다.
	 *
	 * @return 스키마를 따르는 JSON 문자열 (파싱은 호출자의 책임)
	 * @throws AiGenerationException 호출 실패, 또는 응답에서 구조화된 결과를 찾을 수 없을 때
	 */
	String generateStructuredJson(AiGenerationRequest request);
}
