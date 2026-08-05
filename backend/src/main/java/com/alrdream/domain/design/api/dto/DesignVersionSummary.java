package com.alrdream.domain.design.api.dto;

import com.alrdream.domain.design.domain.DesignVersion;
import com.alrdream.domain.design.domain.DesignVersionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.UUID;

@Schema(description = "설계 버전 목록 항목 (content 제외)")
public record DesignVersionSummary(
		@Schema(description = "설계 버전 ID") UUID id,
		@Schema(description = "버전 번호") int versionNo,
		@Schema(description = "생성 상태") DesignVersionStatus status,
		@Schema(description = "생성 시각") OffsetDateTime createdAt,
		@Schema(description = "수정 시각") OffsetDateTime updatedAt) {

	public static DesignVersionSummary of(DesignVersion version) {
		return new DesignVersionSummary(
				version.getId(), version.getVersionNo(), version.getStatus(), version.getCreatedAt(), version.getUpdatedAt());
	}
}
