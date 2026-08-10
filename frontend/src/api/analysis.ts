import { apiClient } from "./client";
import type { AiGenerationJob, AnalysisVersionDetail, AnalysisVersionSummary, DocumentResponse, Page } from "../types";

const base = (workspaceId: string, planningVersionId: string) =>
  `/api/workspaces/${workspaceId}/planning-versions/${planningVersionId}/analysis-versions`;

export const analysisApi = {
  create: (workspaceId: string, planningVersionId: string) =>
    apiClient.post<AiGenerationJob>(base(workspaceId, planningVersionId)),
  list: (workspaceId: string, planningVersionId: string) =>
    apiClient
      .get<Page<AnalysisVersionSummary>>(base(workspaceId, planningVersionId), { size: 50 })
      .then((res) => res.content),
  get: (workspaceId: string, planningVersionId: string, analysisVersionId: string) =>
    apiClient.get<AnalysisVersionDetail>(`${base(workspaceId, planningVersionId)}/${analysisVersionId}`),
  generatePdf: (workspaceId: string, planningVersionId: string, analysisVersionId: string) =>
    apiClient.post<DocumentResponse>(`${base(workspaceId, planningVersionId)}/${analysisVersionId}/pdf`),
  remove: (workspaceId: string, planningVersionId: string, analysisVersionIds: string[]) =>
    apiClient.delete<void>(base(workspaceId, planningVersionId), { analysisVersionIds }),
};
