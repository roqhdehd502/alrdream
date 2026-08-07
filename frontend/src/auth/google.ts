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

export function extractIdToken(response: AuthSession.AuthSessionResult | null): string | null {
  if (response?.type !== "success") return null;
  return response.params.id_token ?? null;
}
