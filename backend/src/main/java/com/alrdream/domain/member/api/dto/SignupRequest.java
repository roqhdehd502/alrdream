package com.alrdream.domain.member.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignupRequest(
		@Schema(description = "이메일", example = "user@example.com") @NotBlank @Email String email,
		@Schema(description = "비밀번호 (8~100자)", example = "password1234") @NotBlank @Size(min = 8, max = 100) String password) {
}
