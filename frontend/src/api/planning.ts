import { apiClient } from "./client";
import type { AiGenerationJob, DocumentResponse, PlanningVersionDetail, PlanningVersionSummary } from "../types";

const base = (workspaceId: string) => `/api/workspaces/${workspaceId}/planning-versions`;

export const planningApi = {
  create: (workspaceId: string, surveyResponseId: string) =>
    apiClient.post<AiGenerationJob>(base(workspaceId), { surveyResponseId }),
  list: (workspaceId: string) => apiClient.get<PlanningVersionSummary[]>(base(workspaceId)),
  get: (workspaceId: string, versionId: string) =>
    apiClient.get<PlanningVersionDetail>(`${base(workspaceId)}/${versionId}`),
  generatePdf: (workspaceId: string, versionId: string) =>
    apiClient.post<DocumentResponse>(`${base(workspaceId)}/${versionId}/pdf`),
  remove: (workspaceId: string, versionIds: string[]) =>
    apiClient.delete<void>(base(workspaceId), { versionIds }),
};
