import { apiClient } from "./client";
import type { UsageQuotaResponse } from "../types";

export const usageQuotaApi = {
  getCurrent: () => apiClient.get<UsageQuotaResponse>("/api/usage-quota/me"),
};
