import { apiClient } from "./client";
import type { RedeemCouponResponse } from "../types";

export const couponApi = {
  redeem: (code: string) => apiClient.post<RedeemCouponResponse>("/api/coupons/redeem", { code }),
};
