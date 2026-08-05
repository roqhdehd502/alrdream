package com.alrdream.domain.workspace.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateWorkspaceRequest(
		@Schema(description = "새 워크스페이스 이름", example = "수정된 사업 아이템") @NotBlank @Size(max = 255) String name) {
}
