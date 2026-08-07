import { apiClient } from "./client";
import type { SurveyAnswer, SurveyDefinition, SurveyKey, SurveyResponseDetail, SurveyResponseSummary } from "../types";

export const surveysApi = {
  get: (workspaceId: string, surveyKey: SurveyKey) =>
    apiClient.get<SurveyDefinition>(`/api/workspaces/${workspaceId}/surveys/${surveyKey}`),
  submit: (workspaceId: string, surveyKey: SurveyKey, answers: SurveyAnswer[]) =>
    apiClient.post<SurveyResponseDetail>(`/api/workspaces/${workspaceId}/survey-responses`, { surveyKey, answers }),
  list: (workspaceId: string) =>
    apiClient.get<SurveyResponseSummary[]>(`/api/workspaces/${workspaceId}/survey-responses`),
  getResponse: (workspaceId: string, responseId: string) =>
    apiClient.get<SurveyResponseDetail>(`/api/workspaces/${workspaceId}/survey-responses/${responseId}`),
};
