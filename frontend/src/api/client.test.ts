import { apiClient, ApiError, setOnUnauthorized } from "./client";
import { tokenStorage } from "./tokenStorage";

jest.mock("./tokenStorage", () => ({
  tokenStorage: {
    load: jest.fn(),
    save: jest.fn(),
    clear: jest.fn(),
  },
}));

function jsonResponse(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), { status, headers: { "Content-Type": "application/json" } });
}

function emptyResponse(status: number): Response {
  return new Response(null, { status });
}

const mockedTokenStorage = jest.mocked(tokenStorage);

describe("apiClient", () => {
  beforeEach(() => {
    jest.resetAllMocks();
    global.fetch = jest.fn();
    setOnUnauthorized(null);
  });

  it("인증 없이 GET하면 토큰 조회 없이 바로 요청한다", async () => {
    mockedTokenStorage.load.mockResolvedValue(null);
    jest.mocked(fetch).mockResolvedValueOnce(jsonResponse(200, { ok: true }));

    const result = await apiClient.get<{ ok: boolean }>("/api/public", undefined);

    expect(result).toEqual({ ok: true });
  });

  it("204 응답은 undefined를 반환한다", async () => {
    mockedTokenStorage.load.mockResolvedValue({ accessToken: "a", refreshToken: "r" });
    jest.mocked(fetch).mockResolvedValueOnce(emptyResponse(204));

    const result = await apiClient.delete("/api/thing");

    expect(result).toBeUndefined();
  });

  it("실패 응답의 code/message를 ApiError로 감싼다", async () => {
    mockedTokenStorage.load.mockResolvedValue({ accessToken: "a", refreshToken: "r" });
    jest.mocked(fetch).mockResolvedValueOnce(jsonResponse(400, { code: "BAD_REQUEST", message: "잘못됨" }));

    await expect(apiClient.get("/api/thing")).rejects.toMatchObject({ status: 400, code: "BAD_REQUEST" });
  });

  it("401을 받으면 토큰을 갱신해 재시도하고 성공하면 결과를 반환한다", async () => {
    mockedTokenStorage.load.mockResolvedValue({ accessToken: "expired", refreshToken: "valid" });
    mockedTokenStorage.save.mockResolvedValue(undefined);
    jest
      .mocked(fetch)
      .mockResolvedValueOnce(emptyResponse(401)) // 최초 요청
      .mockResolvedValueOnce(jsonResponse(200, { accessToken: "new-a", refreshToken: "new-r" })) // refresh
      .mockResolvedValueOnce(jsonResponse(200, { ok: true })); // 재시도

    const result = await apiClient.get<{ ok: boolean }>("/api/thing");

    expect(result).toEqual({ ok: true });
    expect(mockedTokenStorage.save).toHaveBeenCalledWith({ accessToken: "new-a", refreshToken: "new-r" });
  });

  it("refresh도 실패하면 세션을 정리하고 onUnauthorized를 호출한다", async () => {
    mockedTokenStorage.load.mockResolvedValue({ accessToken: "expired", refreshToken: "invalid" });
    mockedTokenStorage.clear.mockResolvedValue(undefined);
    const onUnauthorized = jest.fn();
    setOnUnauthorized(onUnauthorized);
    jest
      .mocked(fetch)
      .mockResolvedValueOnce(emptyResponse(401)) // 최초 요청
      .mockResolvedValueOnce(emptyResponse(400)); // refresh 실패

    await expect(apiClient.get("/api/thing")).rejects.toBeInstanceOf(ApiError);
    expect(mockedTokenStorage.clear).toHaveBeenCalled();
    expect(onUnauthorized).toHaveBeenCalled();
  });

  it("갱신된 토큰으로 재시도해도 401이면 세션을 정리한다 (재시도 후 401 방치 버그의 회귀 방지)", async () => {
    // Phase 15가 고친 버그 — 계정이 정지/삭제된 경우, 재시도가 또 401이면 세션을 정리하지 않고 그냥
    // ApiError만 던져 로그인 상태가 깨진 채 방치됐었다.
    mockedTokenStorage.load.mockResolvedValue({ accessToken: "expired", refreshToken: "valid" });
    mockedTokenStorage.save.mockResolvedValue(undefined);
    mockedTokenStorage.clear.mockResolvedValue(undefined);
    const onUnauthorized = jest.fn();
    setOnUnauthorized(onUnauthorized);
    jest
      .mocked(fetch)
      .mockResolvedValueOnce(emptyResponse(401)) // 최초 요청
      .mockResolvedValueOnce(jsonResponse(200, { accessToken: "new-a", refreshToken: "new-r" })) // refresh 성공
      .mockResolvedValueOnce(emptyResponse(401)); // 새 토큰으로 재시도해도 여전히 401

    await expect(apiClient.get("/api/thing")).rejects.toBeInstanceOf(ApiError);
    expect(mockedTokenStorage.clear).toHaveBeenCalled();
    expect(onUnauthorized).toHaveBeenCalled();
  });

  it("동시에 여러 요청이 401을 받아도 refresh는 한 번만 호출한다", async () => {
    mockedTokenStorage.load.mockResolvedValue({ accessToken: "expired", refreshToken: "valid" });
    mockedTokenStorage.save.mockResolvedValue(undefined);
    jest
      .mocked(fetch)
      .mockResolvedValueOnce(emptyResponse(401)) // 요청 A 최초
      .mockResolvedValueOnce(emptyResponse(401)) // 요청 B 최초
      .mockResolvedValueOnce(jsonResponse(200, { accessToken: "new-a", refreshToken: "new-r" })) // refresh(공유)
      .mockResolvedValueOnce(jsonResponse(200, { a: true })) // 요청 A 재시도
      .mockResolvedValueOnce(jsonResponse(200, { b: true })); // 요청 B 재시도

    const [a, b] = await Promise.all([apiClient.get("/api/a"), apiClient.get("/api/b")]);

    expect(a).toEqual({ a: true });
    expect(b).toEqual({ b: true });
    expect(jest.mocked(fetch)).toHaveBeenCalledTimes(5); // refresh 호출이 2번이 아니라 1번만 나감
  });
});
