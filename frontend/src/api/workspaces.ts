import { apiClient } from "./client";
import type { Page, Workspace } from "../types";

export const workspacesApi = {
  list: (keyword: string | undefined, page: number) =>
    apiClient.get<Page<Workspace>>("/api/workspaces", { keyword, page, size: 20 }),
  get: (workspaceId: string) => apiClient.get<Workspace>(`/api/workspaces/${workspaceId}`),
  create: (name: string) => apiClient.post<Workspace>("/api/workspaces", { name }),
  rename: (workspaceId: string, name: string) => apiClient.patch<Workspace>(`/api/workspaces/${workspaceId}`, { name }),
  remove: (workspaceId: string) => apiClient.delete<void>(`/api/workspaces/${workspaceId}`),
};
