import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { apiFetch, ApiError, notifyLogout, toQueryString } from "./client";
import { tokenStorage } from "./tokenStorage";

function jsonResponse(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), { status, headers: { "Content-Type": "application/json" } });
}

function textResponse(status: number, body: string): Response {
  return new Response(body, { status });
}

function emptyResponse(status: number): Response {
  return new Response(null, { status });
}

describe("apiFetch", () => {
  beforeEach(() => {
    localStorage.clear();
    vi.stubGlobal("fetch", vi.fn());
    // jsdom은 location.href 대입을 실제 내비게이션으로 취급해 "Not implemented" 에러를 낼 수 있어,
    // 테스트에서 관찰 가능한 평범한 객체로 대체한다.
    Object.defineProperty(window, "location", {
      value: { pathname: "/dashboard", href: "" },
      writable: true,
    });
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("성공 응답은 파싱된 JSON을 그대로 반환한다", async () => {
    tokenStorage.setTokens("access-1", "refresh-1");
    vi.mocked(fetch).mockResolvedValueOnce(jsonResponse(200, { hello: "world" }));

    const result = await apiFetch<{ hello: string }>("/api/ping");

    expect(result).toEqual({ hello: "world" });
  });

  it("204 응답은 undefined를 반환한다", async () => {
    vi.mocked(fetch).mockResolvedValueOnce(emptyResponse(204));

    const result = await apiFetch("/api/ping");

    expect(result).toBeUndefined();
  });

  it("실패 응답의 code/message를 ApiError로 그대로 전달한다", async () => {
    vi.mocked(fetch).mockResolvedValueOnce(jsonResponse(400, { code: "BAD_REQUEST", message: "잘못된 요청입니다." }));

    await expect(apiFetch("/api/ping")).rejects.toMatchObject({
      status: 400,
      code: "BAD_REQUEST",
      message: "잘못된 요청입니다.",
    });
  });

  it("JSON이 아닌 에러 응답(프록시 502 HTML 등)도 일관된 ApiError로 감싼다", async () => {
    // 관리자 로그인 페이지 수정 이력(Phase 15) — JSON.parse 실패가 원본 SyntaxError로 그대로 던져지던 버그.
    vi.mocked(fetch).mockResolvedValueOnce(textResponse(502, "<html>Bad Gateway</html>"));

    const error: unknown = await apiFetch("/api/ping").catch((e) => e);

    expect(error).toBeInstanceOf(ApiError);
    expect(error).toMatchObject({ status: 502 });
  });

  it("401 응답을 받으면 refresh 후 새 토큰으로 재시도한다", async () => {
    tokenStorage.setTokens("expired-access", "valid-refresh");
    vi.mocked(fetch)
      .mockResolvedValueOnce(emptyResponse(401)) // 최초 요청
      .mockResolvedValueOnce(jsonResponse(200, { accessToken: "new-access", refreshToken: "new-refresh" })) // refresh
      .mockResolvedValueOnce(jsonResponse(200, { ok: true })); // 재시도

    const result = await apiFetch<{ ok: boolean }>("/api/ping");

    expect(result).toEqual({ ok: true });
    expect(tokenStorage.getAccessToken()).toBe("new-access");
    expect(vi.mocked(fetch)).toHaveBeenCalledTimes(3);
  });

  it("refresh도 실패하면 세션을 정리하고 로그인 페이지로 보낸다", async () => {
    tokenStorage.setTokens("expired-access", "invalid-refresh");
    vi.mocked(fetch)
      .mockResolvedValueOnce(emptyResponse(401)) // 최초 요청
      .mockResolvedValueOnce(emptyResponse(400)); // refresh 실패

    await expect(apiFetch("/api/ping")).rejects.toMatchObject({ status: 401, code: "UNAUTHORIZED" });

    expect(tokenStorage.getAccessToken()).toBeNull();
    expect(window.location.href).toBe("/login");
  });

  it("로그아웃이 진행 중이던 refresh 성공보다 먼저 일어나면 그 결과를 저장하지 않는다", async () => {
    // Phase 15가 고친 버그의 회귀 방지 테스트 — 401 재시도용 refresh가 진행되는 도중 사용자가 로그아웃을
    // 누르면(notifyLogout), 뒤늦게 도착하는 refresh 성공 응답이 tokenStorage를 다시 채워 로그인 상태를
    // 부활시키면 안 된다.
    tokenStorage.setTokens("expired-access", "valid-refresh");
    let resolveRefresh!: (value: Response) => void;
    const pendingRefresh = new Promise<Response>((resolve) => {
      resolveRefresh = resolve;
    });
    vi.mocked(fetch)
      .mockResolvedValueOnce(emptyResponse(401)) // 최초 요청
      .mockReturnValueOnce(pendingRefresh); // refresh — 아직 응답 안 옴

    const pendingApiFetch = apiFetch("/api/ping");

    // apiFetch가 최초 401 응답을 처리하고 refresh 요청까지 실제로 보낼 때까지 마이크로태스크를 흘려보낸다
    // (그래야 refreshAccessToken이 epochAtStart를 캡처한 "이후"에 notifyLogout이 일어나는 정확한 순서가 된다).
    await new Promise((resolve) => setTimeout(resolve, 0));

    // refresh가 응답하기 전에 로그아웃이 먼저 발생했다고 시뮬레이션.
    notifyLogout();
    resolveRefresh(jsonResponse(200, { accessToken: "late-access", refreshToken: "late-refresh" }));

    await expect(pendingApiFetch).rejects.toMatchObject({ status: 401 });
    expect(tokenStorage.getAccessToken()).toBeNull(); // "late-access"로 되살아나지 않음
  });
});

describe("toQueryString", () => {
  it("값이 있는 항목만 쿼리스트링으로 만든다", () => {
    expect(toQueryString({ keyword: "a", page: 1, empty: "", missing: undefined, none: null })).toBe(
      "?keyword=a&page=1",
    );
  });

  it("모든 값이 비어있으면 빈 문자열을 반환한다", () => {
    expect(toQueryString({ a: undefined, b: null, c: "" })).toBe("");
  });
});
