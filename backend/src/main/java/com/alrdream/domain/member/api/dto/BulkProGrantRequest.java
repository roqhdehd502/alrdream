package com.alrdream.domain.member.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.UUID;

public record BulkProGrantRequest(
		@Schema(description = "대상 회원 ID 목록") @NotEmpty List<UUID> userIds,
		@Schema(description = "지급/연장 일수") @Min(1) int days) {
}
