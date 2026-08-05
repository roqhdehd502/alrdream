package com.alrdream.domain.ai.api.dto;

import com.alrdream.domain.ai.domain.AiGenerationJob;
import com.alrdream.domain.ai.domain.AiTargetType;
import com.alrdream.domain.ai.domain.JobStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.UUID;

@Schema(description = "AI 생성 작업(Job) 상태")
public record AiGenerationJobResponse(
		@Schema(description = "작업 ID") UUID id,
		@Schema(description = "생성 결과가 저장될 대상 도메인") AiTargetType targetType,
		@Schema(description = "대상 ID (기획/분석/설계 버전 ID)") UUID targetId,
		@Schema(description = "작업 상태") JobStatus status,
		@Schema(description = "FAILED일 때의 오류 메시지") String errorMessage,
		@Schema(description = "작업 생성 시각") OffsetDateTime createdAt) {

	public static AiGenerationJobResponse of(AiGenerationJob job) {
		return new AiGenerationJobResponse(
				job.getId(),
				job.getTargetType(),
				job.getTargetId(),
				job.getStatus(),
				job.getErrorMessage(),
				job.getCreatedAt());
	}
}
