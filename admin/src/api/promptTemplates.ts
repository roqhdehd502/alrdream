import type { AiTargetType, PromptTemplateResponse } from "../types";
import { apiFetch, toQueryString } from "./client";

export interface PublishPromptTemplateRequest {
  promptType: AiTargetType;
  toolName: string;
  toolDescription: string;
  systemPrompt: string;
  schemaJson: string;
}

export const promptTemplatesApi = {
  list: (promptType?: AiTargetType) =>
    apiFetch<PromptTemplateResponse[]>(`/api/admin/prompt-templates${toQueryString({ promptType })}`),

  get: (id: string) => apiFetch<PromptTemplateResponse>(`/api/admin/prompt-templates/${id}`),

  publish: (request: PublishPromptTemplateRequest) =>
    apiFetch<PromptTemplateResponse>("/api/admin/prompt-templates", {
      method: "POST",
      body: JSON.stringify(request),
    }),
};
