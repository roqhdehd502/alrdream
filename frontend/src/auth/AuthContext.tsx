import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from "react";
import { authApi } from "../api/auth";
import { setOnUnauthorized } from "../api/client";
import { tokenStorage } from "../api/tokenStorage";
import type { Member } from "../types";

type SessionStatus = "loading" | "authenticated" | "unauthenticated";

interface AuthContextValue {
  status: SessionStatus;
  member: Member | null;
  login: (email: string, password: string) => Promise<void>;
  signup: (email: string, password: string) => Promise<void>;
  loginWithGoogle: (idToken: string) => Promise<void>;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [status, setStatus] = useState<SessionStatus>("loading");
  const [member, setMember] = useState<Member | null>(null);

  useEffect(() => {
    const loadSession = async () => {
      const stored = await tokenStorage.load();
      if (!stored) {
        setStatus("unauthenticated");
        return;
      }
      try {
        const me = await authApi.me();
        setMember(me);
        setStatus("authenticated");
      } catch {
        await tokenStorage.clear();
        setMember(null);
        setStatus("unauthenticated");
      }
    };
    loadSession();
  }, []);

  useEffect(() => {
    setOnUnauthorized(() => {
      setMember(null);
      setStatus("unauthenticated");
    });
    return () => setOnUnauthorized(null);
  }, []);

  const afterTokenIssued = useCallback(async (tokens: { accessToken: string; refreshToken: string }) => {
    await tokenStorage.save(tokens);
    const me = await authApi.me();
    setMember(me);
    setStatus("authenticated");
  }, []);

  const login = useCallback(
    async (email: string, password: string) => {
      await afterTokenIssued(await authApi.login(email, password));
    },
    [afterTokenIssued],
  );

  const signup = useCallback(
    async (email: string, password: string) => {
      await afterTokenIssued(await authApi.signup(email, password));
    },
    [afterTokenIssued],
  );

  const loginWithGoogle = useCallback(
    async (idToken: string) => {
      await afterTokenIssued(await authApi.loginWithGoogle(idToken));
    },
    [afterTokenIssued],
  );

  const logout = useCallback(async () => {
    try {
      await authApi.logout();
    } catch {
      // 이미 만료된 토큰이어도 로컬 세션은 정리해야 하므로 실패를 무시한다.
    }
    await tokenStorage.clear();
    setMember(null);
    setStatus("unauthenticated");
  }, []);

  const value = useMemo(
    () => ({ status, member, login, signup, loginWithGoogle, logout }),
    [status, member, login, signup, loginWithGoogle, logout],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth는 AuthProvider 내부에서만 쓸 수 있습니다.");
  return ctx;
}
