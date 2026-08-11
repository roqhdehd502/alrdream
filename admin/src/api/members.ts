import type { MemberAdminResponse, PagedModel, WorkspaceResponse } from "../types";
import { apiFetch, toQueryString } from "./client";

export const membersApi = {
  list: (keyword: string | undefined, page: number) =>
    apiFetch<PagedModel<MemberAdminResponse>>(
      `/api/admin/users${toQueryString({ keyword, page, size: 20 })}`,
    ),

  get: (userId: string) => apiFetch<MemberAdminResponse>(`/api/admin/users/${userId}`),

  workspaces: (userId: string, keyword: string | undefined, page: number) =>
    apiFetch<PagedModel<WorkspaceResponse>>(
      `/api/admin/users/${userId}/workspaces${toQueryString({ keyword, page, size: 20 })}`,
    ),

  bulkGrantPro: (userIds: string[], days: number) =>
    apiFetch<void>("/api/admin/users/pro-grant", {
      method: "POST",
      body: JSON.stringify({ userIds, days }),
    }),

  downgrade: (userId: string) =>
    apiFetch<MemberAdminResponse>(`/api/admin/users/${userId}/downgrade`, { method: "POST" }),

  ban: (userId: string, permanent: boolean, until?: string) =>
    apiFetch<MemberAdminResponse>(`/api/admin/users/${userId}/ban`, {
      method: "POST",
      body: JSON.stringify({ permanent, until }),
    }),

  unban: (userId: string) => apiFetch<MemberAdminResponse>(`/api/admin/users/${userId}/unban`, { method: "POST" }),
};
