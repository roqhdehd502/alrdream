package com.alrdream.infrastructure.ai;

import tools.jackson.databind.JsonNode;

/** {@code type}에 따라 {@code text}(일반 텍스트) 또는 {@code input}(tool_use 강제 시 구조화 결과)만 채워진다. */
record ClaudeContentBlock(String type, String id, String name, String text, JsonNode input) {
}
