package com.alrdream.domain.planning.api.dto;

import com.alrdream.domain.planning.domain.PlanningVersion;
import com.alrdream.domain.planning.domain.PlanningVersionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.UUID;

@Schema(description = "기획안 버전 목록 항목 (content 제외)")
public record PlanningVersionSummary(
		@Schema(description = "기획안 버전 ID") UUID id,
		@Schema(description = "버전 번호") int versionNo,
		@Schema(description = "생성 상태") PlanningVersionStatus status,
		@Schema(description = "생성 시각") OffsetDateTime createdAt,
		@Schema(description = "수정 시각") OffsetDateTime updatedAt) {

	public static PlanningVersionSummary of(PlanningVersion version) {
		return new PlanningVersionSummary(
				version.getId(), version.getVersionNo(), version.getStatus(), version.getCreatedAt(), version.getUpdatedAt());
	}
}
