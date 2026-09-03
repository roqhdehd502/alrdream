import { apiClient } from "./client";
import type { Member, SignupVerificationRequestResponse, TokenPair } from "../types";

export const authApi = {
  signup: (email: string, password: string) =>
    apiClient.post<TokenPair>("/api/auth/signup", { email, password }, { auth: false }),
  login: (email: string, password: string) =>
    apiClient.post<TokenPair>("/api/auth/login", { email, password }, { auth: false }),
  loginWithGoogle: (idToken: string) =>
    apiClient.post<TokenPair>("/api/auth/oauth/google", { idToken }, { auth: false }),
  logout: () => apiClient.post<void>("/api/auth/logout"),
  me: () => apiClient.get<Member>("/api/auth/me"),
  updateName: (name: string) => apiClient.patch<Member>("/api/auth/me", { name }),
  verifyPassword: (password: string) => apiClient.post<void>("/api/auth/me/verify-password", { password }),
  withdraw: () => apiClient.delete<void>("/api/auth/me"),
  requestPasswordReset: (email: string) =>
    apiClient.post<void>("/api/auth/password-reset/request", { email }, { auth: false }),
  confirmPasswordReset: (email: string, code: string, newPassword: string) =>
    apiClient.post<void>("/api/auth/password-reset/confirm", { email, code, newPassword }, { auth: false }),
  requestEmailVerification: () =>
    apiClient.post<SignupVerificationRequestResponse>("/api/auth/email-verification/request"),
  confirmEmailVerification: (code: string) =>
    apiClient.post<void>("/api/auth/email-verification/confirm", { code }),
  requestSignupVerification: (email: string) =>
    apiClient.post<SignupVerificationRequestResponse>(
      "/api/auth/signup/email-verification/request",
      { email },
      { auth: false },
    ),
  confirmSignupVerification: (email: string, code: string) =>
    apiClient.post<void>("/api/auth/signup/email-verification/confirm", { email, code }, { auth: false }),
};
