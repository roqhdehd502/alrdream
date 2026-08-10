import type { ErrorResponse, TokenResponse } from "../types";
import { tokenStorage } from "./tokenStorage";

const BASE_URL = import.meta.env.VITE_API_BASE_URL as string;

export class ApiError extends Error {
  status: number;
  code: string;

  constructor(status: number, code: string, message: string) {
    super(message);
    this.status = status;
    this.code = code;
  }
}

function buildHeaders(accessToken: string | null, hasBody: boolean): Headers {
  const headers = new Headers();
  if (hasBody) headers.set("Content-Type", "application/json");
  if (accessToken) headers.set("Authorization", `Bearer ${accessToken}`);
  return headers;
}

// refresh token은 회전(rotation)되어 한 번 쓰면 이전 값이 즉시 무효화된다 — 401이 동시에 여러 건
// 터졌을 때 각자 refresh를 호출하면 서로의 새 토큰을 무효화시키는 경합이 생기므로, 진행 중인 refresh를
// 공유해서 한 번만 실제로 호출한다.
let refreshInFlight: Promise<string | null> | null = null;

// 로그아웃이 진행 중인 refresh 호출 도중에 일어나면(예: 401 재시도 중 사용자가 로그아웃 클릭), refresh가
// 뒤늦게 성공해 tokenStorage에 새 토큰을 다시 채워넣어 "로그아웃했는데 새로고침하면 다시 로그인돼 있는"
// 상태가 될 수 있다. logout()이 이 카운터를 올려 그런 뒤늦은 저장을 무시하게 한다.
let logoutEpoch = 0;

export function notifyLogout(): void {
  logoutEpoch += 1;
}

async function refreshAccessToken(): Promise<string | null> {
  if (refreshInFlight) return refreshInFlight;

  const epochAtStart = logoutEpoch;
  refreshInFlight = (async () => {
    const refreshToken = tokenStorage.getRefreshToken();
    if (!refreshToken) return null;
    try {
      const res = await fetch(`${BASE_URL}/api/auth/refresh`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ refreshToken }),
      });
      if (!res.ok) return null;
      const data: TokenResponse = await res.json();
      if (logoutEpoch !== epochAtStart) return null;
      tokenStorage.setTokens(data.accessToken, data.refreshToken);
      return data.accessToken;
    } catch {
      return null;
    }
  })();

  try {
    return await refreshInFlight;
  } finally {
    refreshInFlight = null;
  }
}

export async function apiFetch<T>(path: string, options: RequestInit = {}): Promise<T> {
  const hasBody = options.body !== undefined;
  let accessToken = tokenStorage.getAccessToken();
  let res = await fetch(`${BASE_URL}${path}`, { ...options, headers: buildHeaders(accessToken, hasBody) });

  if (res.status === 401 && accessToken) {
    const newToken = await refreshAccessToken();
    if (!newToken) {
      tokenStorage.clear();
      if (window.location.pathname !== "/login") {
        window.location.href = "/login";
      }
      throw new ApiError(401, "UNAUTHORIZED", "인증이 만료되었습니다. 다시 로그인해주세요.");
    }
    res = await fetch(`${BASE_URL}${path}`, { ...options, headers: buildHeaders(newToken, hasBody) });
  }

  if (res.status === 204) {
    return undefined as T;
  }

  const text = await res.text();
  let data: unknown;
  try {
    data = text.length > 0 ? JSON.parse(text) : undefined;
  } catch {
    // 프록시/로드밸런서가 JSON이 아닌 본문(예: 502/504 HTML 에러 페이지)을 반환한 경우 — 원문 대신
    // 일관된 형식의 에러로 감싼다.
    if (!res.ok) {
      throw new ApiError(res.status, "UNKNOWN", `요청이 실패했습니다 (HTTP ${res.status})`);
    }
    data = undefined;
  }

  if (!res.ok) {
    const err = data as ErrorResponse | undefined;
    throw new ApiError(res.status, err?.code ?? "UNKNOWN", err?.message ?? `요청이 실패했습니다 (HTTP ${res.status})`);
  }

  return data as T;
}

export function toQueryString(params: Record<string, string | number | undefined | null>): string {
  const search = new URLSearchParams();
  for (const [key, value] of Object.entries(params)) {
    if (value !== undefined && value !== null && value !== "") {
      search.set(key, String(value));
    }
  }
  const query = search.toString();
  return query ? `?${query}` : "";
}
