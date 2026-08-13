package com.alrdream.domain.member.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record VerifyPasswordRequest(
		@Schema(description = "현재 비밀번호") @NotBlank String password) {
}
