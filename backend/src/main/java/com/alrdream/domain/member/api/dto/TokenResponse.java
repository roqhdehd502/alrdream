package com.alrdream.domain.member.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record TokenResponse(
		@Schema(description = "API 인증에 사용하는 access token (30분 유효)") String accessToken,
		@Schema(description = "access token 갱신에 사용하는 refresh token (14일 유효)") String refreshToken) {
}
