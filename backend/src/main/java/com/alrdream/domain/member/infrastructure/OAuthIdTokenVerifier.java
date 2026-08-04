package com.alrdream.domain.member.infrastructure;

/**
 * [03] §4-5 소셜 로그인 — Frontend(Expo)가 각 provider SDK로 발급받은 ID 토큰을 백엔드가 검증하는 방식.
 * Spring의 OAuth2Client 리다이렉트 플로우 대신 이 방식을 쓰는 이유: 모바일 앱은 서버로의 브라우저 리다이렉트가
 * 자연스럽지 않고, 클라이언트가 각 provider의 네이티브 SDK로 직접 ID 토큰을 받아 백엔드에 전달하는 편이
 * UX/구현 모두 더 적합하다.
 */
public interface OAuthIdTokenVerifier {

	/** ID 토큰의 서명·발급자·audience·만료를 검증하고 사용자 식별 정보를 반환한다. 검증 실패 시 {@link IllegalArgumentException}. */
	OAuthUserInfo verify(String idToken);
}
