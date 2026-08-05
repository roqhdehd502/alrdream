package com.alrdream.infrastructure.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import tools.jackson.databind.JsonNode;

record ClaudeTool(String name, String description, @JsonProperty("input_schema") JsonNode inputSchema) {
}
