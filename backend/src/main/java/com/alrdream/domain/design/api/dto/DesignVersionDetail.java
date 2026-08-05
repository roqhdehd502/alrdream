package com.alrdream.domain.design.api.dto;

import com.alrdream.domain.design.domain.DesignVersion;
import com.alrdream.domain.design.domain.DesignVersionStatus;
import com.fasterxml.jackson.annotation.JsonRawValue;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.UUID;

@Schema(description = "설계 버전 상세")
public record DesignVersionDetail(
		@Schema(description = "설계 버전 ID") UUID id,
		@Schema(description = "이 설계의 기반이 된 분석 버전 ID") UUID analysisVersionId,
		@Schema(description = "이 버전을 생성한 DESIGN 설문 응답 ID") UUID surveyResponseId,
		@Schema(description = "버전 번호") int versionNo,
		@Schema(description = "생성 상태") DesignVersionStatus status,
		@Schema(description = "생성 결과 (GENERATING 중이거나 FAILED면 null)") @JsonRawValue String content,
		@Schema(description = "생성 시각") OffsetDateTime createdAt,
		@Schema(description = "수정 시각") OffsetDateTime updatedAt) {

	public static DesignVersionDetail of(DesignVersion version) {
		return new DesignVersionDetail(
				version.getId(),
				version.getAnalysisVersionId(),
				version.getSurveyResponseId(),
				version.getVersionNo(),
				version.getStatus(),
				version.getContent(),
				version.getCreatedAt(),
				version.getUpdatedAt());
	}
}
