package com.alrdream.domain.design.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.UUID;

public record DeleteDesignVersionsRequest(
		@Schema(description = "삭제할 설계 버전 ID 목록") @NotEmpty List<UUID> designVersionIds) {
}
