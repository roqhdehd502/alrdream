package com.alrdream.infrastructure.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/** Anthropic Messages API(POST /v1/messages) 요청 바디. Tool Use를 {@code tool_choice}로 강제해 구조화 출력을 확보한다. */
record ClaudeMessagesRequest(
		String model,
		@JsonProperty("max_tokens") int maxTokens,
		String system,
		List<ClaudeMessage> messages,
		List<ClaudeTool> tools,
		@JsonProperty("tool_choice") ClaudeToolChoice toolChoice) {
}
