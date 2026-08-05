package com.alrdream.domain.analysis.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.UUID;

/** [01] 9번 — 다중 선택 삭제. */
public record DeleteAnalysisVersionsRequest(
		@Schema(description = "삭제할 분석 버전 ID 목록") @NotEmpty List<UUID> analysisVersionIds) {
}
