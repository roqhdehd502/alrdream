import { apiClient } from "./client";
import type { AiGenerationJob, DesignVersionDetail, DesignVersionSummary, DocumentResponse } from "../types";

const base = (workspaceId: string, planningVersionId: string, analysisVersionId: string) =>
  `/api/workspaces/${workspaceId}/planning-versions/${planningVersionId}` +
  `/analysis-versions/${analysisVersionId}/design-versions`;

export const designApi = {
  create: (workspaceId: string, planningVersionId: string, analysisVersionId: string, surveyResponseId: string) =>
    apiClient.post<AiGenerationJob>(base(workspaceId, planningVersionId, analysisVersionId), { surveyResponseId }),
  list: (workspaceId: string, planningVersionId: string, analysisVersionId: string) =>
    apiClient.get<DesignVersionSummary[]>(base(workspaceId, planningVersionId, analysisVersionId)),
  get: (workspaceId: string, planningVersionId: string, analysisVersionId: string, designVersionId: string) =>
    apiClient.get<DesignVersionDetail>(`${base(workspaceId, planningVersionId, analysisVersionId)}/${designVersionId}`),
  generatePdf: (workspaceId: string, planningVersionId: string, analysisVersionId: string, designVersionId: string) =>
    apiClient.post<DocumentResponse>(
      `${base(workspaceId, planningVersionId, analysisVersionId)}/${designVersionId}/pdf`,
    ),
  remove: (workspaceId: string, planningVersionId: string, analysisVersionId: string, designVersionIds: string[]) =>
    apiClient.delete<void>(base(workspaceId, planningVersionId, analysisVersionId), { designVersionIds }),
};
