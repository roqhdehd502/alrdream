package com.alrdream.domain.workspace.api.dto;

import com.alrdream.domain.workspace.domain.Workspace;
import com.alrdream.domain.workspace.domain.WorkspaceStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.UUID;

public record WorkspaceResponse(
		@Schema(description = "워크스페이스 ID") UUID id,
		@Schema(description = "워크스페이스 이름") String name,
		@Schema(description = "상태") WorkspaceStatus status,
		@Schema(description = "생성 시각") OffsetDateTime createdAt,
		@Schema(description = "수정 시각") OffsetDateTime updatedAt) {

	public static WorkspaceResponse from(Workspace workspace) {
		return new WorkspaceResponse(
				workspace.getId(),
				workspace.getName(),
				workspace.getStatus(),
				workspace.getCreatedAt(),
				workspace.getUpdatedAt());
	}
}
