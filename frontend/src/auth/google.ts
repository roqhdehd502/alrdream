import * as AuthSession from "expo-auth-session";
import * as Crypto from "expo-crypto";
import * as WebBrowser from "expo-web-browser";

// web에서 팝업을 닫으려면 모듈 스코프에서 한 번 호출해둬야 한다 (SDK 57 문서).
WebBrowser.maybeCompleteAuthSession();

const GOOGLE_DISCOVERY = {
  authorizationEndpoint: "https://accounts.google.com/o/oauth2/v2/auth",
};

/**
 * Google Cloud Console에 등록된 client-id(웹 타입)로 id_token을 직접 발급받는 implicit 플로우.
 * `@react-native-google-signin/google-signin`(Expo 공식 권장)은 커스텀 네이티브 코드가 필요해 Expo Go/웹에서
 * 못 쓰므로, 대신 expo-auth-session으로 웹/Expo Go 양쪽에서 동작하는 이 방식을 쓴다. 단, Google Cloud Console에
 * 실행 환경별 redirect URI(웹 배포 도메인, Expo Go 프록시 등)가 등록돼 있어야 실제로 동작한다.
 */
export function useGoogleAuthRequest() {
  const redirectUri = AuthSession.makeRedirectUri();
  return AuthSession.useAuthRequest(
    {
      clientId: process.env.EXPO_PUBLIC_GOOGLE_CLIENT_ID ?? "",
      scopes: ["openid", "profile", "email"],
      redirectUri,
      responseType: AuthSession.ResponseType.IdToken,
      usePKCE: false,
      extraParams: { nonce: Crypto.randomUUID() },
    },
    GOOGLE_DISCOVERY,
  );
}

function decodeJwtPayload(token: string): Record<string, unknown> | null {
  try {
    const payload = token.split(".")[1];
    if (!payload) return null;
    const base64 = payload.replace(/-/g, "+").replace(/_/g, "/");
    return JSON.parse(atob(base64));
  } catch {
    return null;
  }
}

/**
 * id_token을 반환하되, 우리가 이 요청에 실어 보낸 nonce가 토큰의 nonce 클레임과 일치하는지 먼저 확인한다.
 * (서명 자체의 진위는 백엔드가 Google 공개키로 검증하므로 여기서는 서명 검증 없이 페이로드만 읽는다.)
 * 불일치/누락 시 null을 반환해 로그인을 거부한다 — 리다이렉트 콜백에 다른 인증 흐름의 토큰이 주입/재생되는
 * 것을 막는 OIDC nonce의 원래 목적대로, 이 검사는 서버가 아니라 "이 브라우저 세션이 자신이 시작한 요청의
 * 결과를 받았는지"를 확인하는 클라이언트 쪽 책임이다.
 */
export function extractIdToken(
  request: AuthSession.AuthRequest | null,
  response: AuthSession.AuthSessionResult | null,
): string | null {
  if (response?.type !== "success") return null;
  const idToken = response.params.id_token ?? null;
  if (!idToken) return null;

  const expectedNonce = request?.extraParams?.nonce;
  const payload = decodeJwtPayload(idToken);
  if (!expectedNonce || !payload || payload.nonce !== expectedNonce) {
    return null;
  }
  return idToken;
}
