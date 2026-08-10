package com.alrdream.domain.member.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record PasswordResetRequestRequest(
		@Schema(description = "재설정 코드를 받을 이메일", example = "user@example.com") @NotBlank @Email String email) {
}
