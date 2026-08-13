import type { FreeTierLimitResponse } from "../types";
import { apiFetch } from "./client";

export const settingsApi = {
  getFreeTierLimit: () => apiFetch<FreeTierLimitResponse>("/api/admin/settings/free-tier-limit"),

  updateFreeTierLimit: (freeMonthlyLimit: number, proMonthlyLimit: number) =>
    apiFetch<FreeTierLimitResponse>("/api/admin/settings/free-tier-limit", {
      method: "PUT",
      body: JSON.stringify({ freeMonthlyLimit, proMonthlyLimit }),
    }),
};
