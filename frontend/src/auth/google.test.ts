import { extractIdToken } from "./google";
import type * as AuthSession from "expo-auth-session";

// atob는 웹/브라우저 전역 API라 Jest(Node) 환경엔 기본으로 없을 수 있어 폴리필한다.
if (typeof atob === "undefined") {
  (global as { atob?: (data: string) => string }).atob = (data: string) => Buffer.from(data, "base64").toString("binary");
}

function makeIdToken(payload: Record<string, unknown>): string {
  const encode = (obj: unknown) =>
    Buffer.from(JSON.stringify(obj)).toString("base64").replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
  return `${encode({ alg: "RS256" })}.${encode(payload)}.fake-signature`;
}

function requestWithNonce(nonce: string | undefined): AuthSession.AuthRequest {
  return { extraParams: { nonce } } as unknown as AuthSession.AuthRequest;
}

function successResponse(idToken: string | undefined): AuthSession.AuthSessionResult {
  return { type: "success", params: { id_token: idToken } } as unknown as AuthSession.AuthSessionResult;
}

describe("extractIdToken", () => {
  it("nonce가 일치하면 id_token을 반환한다", () => {
    const idToken = makeIdToken({ nonce: "expected-nonce" });

    const result = extractIdToken(requestWithNonce("expected-nonce"), successResponse(idToken));

    expect(result).toBe(idToken);
  });

  it("nonce가 불일치하면 null을 반환한다 (재생 공격 방지 — Phase 15에서 추가된 검증)", () => {
    const idToken = makeIdToken({ nonce: "attacker-nonce" });

    const result = extractIdToken(requestWithNonce("expected-nonce"), successResponse(idToken));

    expect(result).toBeNull();
  });

  it("토큰에 nonce 클레임 자체가 없으면 null을 반환한다", () => {
    const idToken = makeIdToken({ sub: "user-1" }); // nonce 클레임 누락

    const result = extractIdToken(requestWithNonce("expected-nonce"), successResponse(idToken));

    expect(result).toBeNull();
  });

  it("요청에 저장된 기대 nonce가 없으면 null을 반환한다", () => {
    const idToken = makeIdToken({ nonce: "whatever" });

    const result = extractIdToken(requestWithNonce(undefined), successResponse(idToken));

    expect(result).toBeNull();
  });

  it("응답 type이 success가 아니면 null을 반환한다", () => {
    const response = { type: "cancel" } as unknown as AuthSession.AuthSessionResult;

    const result = extractIdToken(requestWithNonce("expected-nonce"), response);

    expect(result).toBeNull();
  });

  it("id_token 자체가 없으면 null을 반환한다", () => {
    const result = extractIdToken(requestWithNonce("expected-nonce"), successResponse(undefined));

    expect(result).toBeNull();
  });

  it("request가 null이면 null을 반환한다", () => {
    const idToken = makeIdToken({ nonce: "whatever" });

    const result = extractIdToken(null, successResponse(idToken));

    expect(result).toBeNull();
  });
});
