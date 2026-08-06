import type { PagedModel, SubscriptionAdminResponse, SubscriptionStatus, SubscriptionSummaryResponse } from "../types";
import { apiFetch, toQueryString } from "./client";

export const subscriptionsApi = {
  list: (status: SubscriptionStatus | undefined, page: number) =>
    apiFetch<PagedModel<SubscriptionAdminResponse>>(
      `/api/admin/subscriptions${toQueryString({ status, page, size: 20 })}`,
    ),

  summary: () => apiFetch<SubscriptionSummaryResponse>("/api/admin/subscriptions/summary"),
};
