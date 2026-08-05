package com.alrdream.domain.analysis.api.dto;

import com.alrdream.domain.analysis.domain.AnalysisVersion;
import com.alrdream.domain.analysis.domain.AnalysisVersionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.UUID;

@Schema(description = "분석 버전 목록 항목 (content 제외)")
public record AnalysisVersionSummary(
		@Schema(description = "분석 버전 ID") UUID id,
		@Schema(description = "버전 번호") int versionNo,
		@Schema(description = "생성 상태") AnalysisVersionStatus status,
		@Schema(description = "생성 시각") OffsetDateTime createdAt,
		@Schema(description = "수정 시각") OffsetDateTime updatedAt) {

	public static AnalysisVersionSummary of(AnalysisVersion version) {
		return new AnalysisVersionSummary(
				version.getId(), version.getVersionNo(), version.getStatus(), version.getCreatedAt(), version.getUpdatedAt());
	}
}
