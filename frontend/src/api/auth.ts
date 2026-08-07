import { apiClient } from "./client";
import type { Member, TokenPair } from "../types";

export const authApi = {
  signup: (email: string, password: string) =>
    apiClient.post<TokenPair>("/api/auth/signup", { email, password }, { auth: false }),
  login: (email: string, password: string) =>
    apiClient.post<TokenPair>("/api/auth/login", { email, password }, { auth: false }),
  loginWithGoogle: (idToken: string) =>
    apiClient.post<TokenPair>("/api/auth/oauth/google", { idToken }, { auth: false }),
  logout: () => apiClient.post<void>("/api/auth/logout"),
  me: () => apiClient.get<Member>("/api/auth/me"),
};
