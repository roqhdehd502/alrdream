import type { MemberResponse, TokenResponse } from "../types";
import { apiFetch } from "./client";

export const authApi = {
  login: (email: string, password: string) =>
    apiFetch<TokenResponse>("/api/auth/login", {
      method: "POST",
      body: JSON.stringify({ email, password }),
    }),

  me: () => apiFetch<MemberResponse>("/api/auth/me"),

  logout: () => apiFetch<void>("/api/auth/logout", { method: "POST" }),
};
