import { createContext, useCallback, useContext, useEffect, useState, type ReactNode } from "react";
import { authApi } from "../api/auth";
import { ApiError, notifyLogout } from "../api/client";
import { tokenStorage } from "../api/tokenStorage";
import type { MemberResponse } from "../types";

type AuthStatus = "loading" | "authenticated" | "unauthenticated";

interface AuthContextValue {
  status: AuthStatus;
  member: MemberResponse | null;
  login: (email: string, password: string) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [status, setStatus] = useState<AuthStatus>("loading");
  const [member, setMember] = useState<MemberResponse | null>(null);

  const loadMe = useCallback(async () => {
    if (!tokenStorage.getAccessToken()) {
      setStatus("unauthenticated");
      return;
    }
    try {
      const me = await authApi.me();
      setMember(me);
      setStatus("authenticated");
    } catch {
      tokenStorage.clear();
      setMember(null);
      setStatus("unauthenticated");
    }
  }, []);

  useEffect(() => {
    loadMe();
  }, [loadMe]);

  const login = useCallback(async (email: string, password: string) => {
    const tokens = await authApi.login(email, password);
    tokenStorage.setTokens(tokens.accessToken, tokens.refreshToken);
    const me = await authApi.me();
    if (me.role !== "ADMIN") {
      tokenStorage.clear();
      setMember(null);
      setStatus("unauthenticated");
      throw new ApiError(403, "FORBIDDEN", "관리자 계정이 아닙니다.");
    }
    setMember(me);
    setStatus("authenticated");
  }, []);

  const logout = useCallback(() => {
    notifyLogout();
    authApi.logout().catch(() => {
      // 로그아웃은 클라이언트 상태 정리가 핵심이라 서버 호출 실패(이미 만료된 토큰 등)는 무시한다.
    });
    tokenStorage.clear();
    setMember(null);
    setStatus("unauthenticated");
  }, []);

  return (
    <AuthContext.Provider value={{ status, member, login, logout }}>{children}</AuthContext.Provider>
  );
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within AuthProvider");
  return ctx;
}
