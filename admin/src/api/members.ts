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
};
