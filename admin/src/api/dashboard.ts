import type { DashboardSummaryResponse } from "../types";
import { apiFetch } from "./client";

export const dashboardApi = {
  summary: () => apiFetch<DashboardSummaryResponse>("/api/admin/dashboard/summary"),
};
