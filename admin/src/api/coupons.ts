import type { CouponRedemptionAdminResponse, CouponResponse, PagedModel } from "../types";
import { apiFetch, toQueryString } from "./client";

export const couponsApi = {
  list: (page: number) => apiFetch<PagedModel<CouponResponse>>(`/api/admin/coupons${toQueryString({ page, size: 20 })}`),

  create: (input: { code: string; benefitDays: number; maxRedemptions?: number; expiresAt?: string }) =>
    apiFetch<CouponResponse>("/api/admin/coupons", {
      method: "POST",
      body: JSON.stringify(input),
    }),

  deactivate: (couponId: string) =>
    apiFetch<CouponResponse>(`/api/admin/coupons/${couponId}/deactivate`, { method: "PATCH" }),

  redemptions: (page: number) =>
    apiFetch<PagedModel<CouponRedemptionAdminResponse>>(
      `/api/admin/coupons/redemptions${toQueryString({ page, size: 20 })}`,
    ),
};
