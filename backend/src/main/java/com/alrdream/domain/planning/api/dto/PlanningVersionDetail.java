package com.alrdream.domain.planning.api.dto;

import com.alrdream.domain.planning.domain.PlanningVersion;
import com.alrdream.domain.planning.domain.PlanningVersionStatus;
import com.fasterxml.jackson.annotation.JsonRawValue;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.UUID;

@Schema(description = "기획안 버전 상세")
public record PlanningVersionDetail(
		@Schema(description = "기획안 버전 ID") UUID id,
		@Schema(description = "워크스페이스 ID") UUID workspaceId,
		@Schema(description = "이 버전을 생성한 설문 응답 ID") UUID surveyResponseId,
		@Schema(description = "버전 번호") int versionNo,
		@Schema(description = "생성 상태") PlanningVersionStatus status,
		@Schema(description = "[01] 12-4 구조의 생성 결과 (GENERATING 중이거나 FAILED면 null)")
				@JsonRawValue String content,
		@Schema(description = "생성 시각") OffsetDateTime createdAt,
		@Schema(description = "수정 시각") OffsetDateTime updatedAt) {

	public static PlanningVersionDetail of(PlanningVersion version) {
		return new PlanningVersionDetail(
				version.getId(),
				version.getWorkspaceId(),
				version.getSurveyResponseId(),
				version.getVersionNo(),
				version.getStatus(),
				version.getContent(),
				version.getCreatedAt(),
				version.getUpdatedAt());
	}
}
