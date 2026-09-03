package com.alrdream.domain.member.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record EmailVerificationRequestResponse(
		@Schema(description = "코드가 만료되기까지 남은 초") int expiresInSeconds) {
}
