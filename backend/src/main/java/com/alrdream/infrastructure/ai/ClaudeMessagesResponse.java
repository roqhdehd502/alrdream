package com.alrdream.infrastructure.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

record ClaudeMessagesResponse(
		String id,
		String type,
		String role,
		String model,
		List<ClaudeContentBlock> content,
		@JsonProperty("stop_reason") String stopReason) {
}
