package com.alrdream.domain.member.api.dto;

import jakarta.validation.constraints.NotBlank;

/** {@code idToken}: Google/Apple 각 provider SDK가 클라이언트에 발급한 ID 토큰(Apple은 identityToken). */
public record OAuthLoginRequest(@NotBlank String idToken) {
}
