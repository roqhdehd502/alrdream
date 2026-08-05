package com.alrdream.infrastructure.ai;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** [03] §4-3 — {@link AiClient}의 1차(현재는 유일한) 구현체. Claude Tool Use를 {@code tool_choice}로 강제해 구조화 출력을 확보한다. */
@Component
public class ClaudeAiClient implements AiClient {

	private final JsonMapper jsonMapper = JsonMapper.builder().build();
	private final ClaudeApi claudeApi;
	private final String model;
	private final int maxTokens;

	public ClaudeAiClient(
			ClaudeApi claudeApi,
			@Value("${app.ai.claude.model}") String model,
			@Value("${app.ai.claude.max-tokens}") int maxTokens) {
		this.claudeApi = claudeApi;
		this.model = model;
		this.maxTokens = maxTokens;
	}

	@Override
	public String generateStructuredJson(AiGenerationRequest request) {
		JsonNode inputSchema;
		try {
			inputSchema = jsonMapper.readTree(request.inputSchemaJson());
		} catch (JacksonException e) {
			throw new AiGenerationException("입력 스키마 JSON이 올바르지 않습니다.", e);
		}

		ClaudeMessagesRequest body = new ClaudeMessagesRequest(
				model,
				maxTokens,
				request.systemPrompt(),
				List.of(new ClaudeMessage("user", request.userPrompt())),
				List.of(new ClaudeTool(request.toolName(), request.toolDescription(), inputSchema)),
				new ClaudeToolChoice("tool", request.toolName()));

		ClaudeMessagesResponse response;
		try {
			response = claudeApi.createMessage(body);
		} catch (RestClientException e) {
			throw new AiGenerationException("Claude API 호출에 실패했습니다: " + e.getMessage(), e);
		}

		JsonNode toolInput = response.content().stream()
				.filter(block -> "tool_use".equals(block.type()) && request.toolName().equals(block.name()))
				.map(ClaudeContentBlock::input)
				.findFirst()
				.orElseThrow(() -> new AiGenerationException(
						"Claude 응답에서 구조화된 결과를 찾을 수 없습니다: stop_reason=" + response.stopReason()));

		try {
			return jsonMapper.writeValueAsString(toolInput);
		} catch (JacksonException e) {
			throw new AiGenerationException("Claude 응답 직렬화에 실패했습니다.", e);
		}
	}
}
