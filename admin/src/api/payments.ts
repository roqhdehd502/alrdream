import type { PagedModel, PaymentAdminResponse, PaymentStatus } from "../types";
import { apiFetch, toQueryString } from "./client";

export const paymentsApi = {
  list: (status: PaymentStatus | undefined, page: number) =>
    apiFetch<PagedModel<PaymentAdminResponse>>(
      `/api/admin/payments${toQueryString({ status, page, size: 20 })}`,
    ),

  listForUser: (userId: string, page: number) =>
    apiFetch<PagedModel<PaymentAdminResponse>>(
      `/api/admin/users/${userId}/payments${toQueryString({ page, size: 20 })}`,
    ),
};
