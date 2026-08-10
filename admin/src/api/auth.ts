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

  requestPasswordReset: (email: string) =>
    apiFetch<void>("/api/auth/password-reset/request", {
      method: "POST",
      body: JSON.stringify({ email }),
    }),

  confirmPasswordReset: (email: string, code: string, newPassword: string) =>
    apiFetch<void>("/api/auth/password-reset/confirm", {
      method: "POST",
      body: JSON.stringify({ email, code, newPassword }),
    }),
};
