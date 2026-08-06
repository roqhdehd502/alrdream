import type { FreeTierLimitResponse } from "../types";
import { apiFetch } from "./client";

export const settingsApi = {
  getFreeTierLimit: () => apiFetch<FreeTierLimitResponse>("/api/admin/settings/free-tier-limit"),

  updateFreeTierLimit: (monthlyLimit: number) =>
    apiFetch<FreeTierLimitResponse>("/api/admin/settings/free-tier-limit", {
      method: "PUT",
      body: JSON.stringify({ monthlyLimit }),
    }),
};
