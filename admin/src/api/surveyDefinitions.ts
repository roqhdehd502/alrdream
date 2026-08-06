import type { Question, SurveyDefinitionResponse, SurveyKey } from "../types";
import { apiFetch, toQueryString } from "./client";

export interface PublishSurveyDefinitionRequest {
  surveyKey: SurveyKey;
  title: string;
  questions: Question[];
}

export const surveyDefinitionsApi = {
  list: (surveyKey?: SurveyKey) =>
    apiFetch<SurveyDefinitionResponse[]>(`/api/admin/survey-definitions${toQueryString({ surveyKey })}`),

  get: (id: string) => apiFetch<SurveyDefinitionResponse>(`/api/admin/survey-definitions/${id}`),

  publish: (request: PublishSurveyDefinitionRequest) =>
    apiFetch<SurveyDefinitionResponse>("/api/admin/survey-definitions", {
      method: "POST",
      body: JSON.stringify(request),
    }),
};
