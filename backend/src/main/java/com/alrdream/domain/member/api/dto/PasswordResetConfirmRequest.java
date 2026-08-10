package com.alrdream.domain.member.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PasswordResetConfirmRequest(
		@Schema(description = "이메일", example = "user@example.com") @NotBlank @Email String email,
		@Schema(description = "이메일로 받은 6자리 코드", example = "123456") @NotBlank @Pattern(regexp = "\\d{6}") String code,
		@Schema(description = "새 비밀번호 (8~100자)", example = "newPassword1234")
				@NotBlank @Size(min = 8, max = 100) String newPassword) {
}
