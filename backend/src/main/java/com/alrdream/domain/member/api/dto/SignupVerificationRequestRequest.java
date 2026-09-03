package com.alrdream.domain.member.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record SignupVerificationRequestRequest(
		@Schema(description = "인증 코드를 받을 이메일") @NotBlank @Email String email) {
}
