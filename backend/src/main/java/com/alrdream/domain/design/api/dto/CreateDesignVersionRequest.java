package com.alrdream.domain.design.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateDesignVersionRequest(
		@Schema(description = "설계 생성/수정에 사용할 DESIGN 설문 응답 ID") @NotNull UUID surveyResponseId) {
}
