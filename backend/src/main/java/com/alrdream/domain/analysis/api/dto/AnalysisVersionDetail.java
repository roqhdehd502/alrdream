package com.alrdream.domain.analysis.api.dto;

import com.alrdream.domain.analysis.domain.AnalysisVersion;
import com.alrdream.domain.analysis.domain.AnalysisVersionStatus;
import com.fasterxml.jackson.annotation.JsonRawValue;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.UUID;

@Schema(description = "분석 버전 상세")
public record AnalysisVersionDetail(
		@Schema(description = "분석 버전 ID") UUID id,
		@Schema(description = "이 분석의 기반이 된 기획안 버전 ID") UUID planningVersionId,
		@Schema(description = "버전 번호") int versionNo,
		@Schema(description = "생성 상태") AnalysisVersionStatus status,
		@Schema(description = "생성 결과 (GENERATING 중이거나 FAILED면 null)") @JsonRawValue String content,
		@Schema(description = "생성 시각") OffsetDateTime createdAt,
		@Schema(description = "수정 시각") OffsetDateTime updatedAt) {

	public static AnalysisVersionDetail of(AnalysisVersion version) {
		return new AnalysisVersionDetail(
				version.getId(),
				version.getPlanningVersionId(),
				version.getVersionNo(),
				version.getStatus(),
				version.getContent(),
				version.getCreatedAt(),
				version.getUpdatedAt());
	}
}
