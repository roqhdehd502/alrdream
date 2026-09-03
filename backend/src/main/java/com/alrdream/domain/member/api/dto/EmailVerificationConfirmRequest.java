package com.alrdream.domain.member.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record EmailVerificationConfirmRequest(
		@Schema(description = "이메일로 받은 6자리 코드") @NotBlank @Pattern(regexp = "\\d{6}") String code) {
}
