package com.alrdream.domain.member.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record SignupVerificationConfirmRequest(
		@Schema(description = "인증 코드를 받은 이메일") @NotBlank @Email String email,
		@Schema(description = "이메일로 받은 6자리 코드") @NotBlank @Pattern(regexp = "\\d{6}") String code) {
}
