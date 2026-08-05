package com.alrdream.domain.planning.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.UUID;

/** [01] 6번 — 다중 선택 삭제. */
public record DeletePlanningVersionsRequest(
		@Schema(description = "삭제할 기획안 버전 ID 목록") @NotEmpty List<UUID> versionIds) {
}
