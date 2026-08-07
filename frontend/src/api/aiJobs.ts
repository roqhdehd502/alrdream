import { apiClient } from "./client";
import type { AiGenerationJob } from "../types";

export const aiJobsApi = {
  get: (jobId: string) => apiClient.get<AiGenerationJob>(`/api/ai-generation-jobs/${jobId}`),
};
