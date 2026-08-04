package com.alrdream.domain.member.infrastructure;

/** OAuth ID 토큰 검증 성공 시 추출되는 최소 정보. */
public record OAuthUserInfo(String providerId, String email) {
}
