import type { SubscriptionPricingResponse } from "../types";
import { apiFetch } from "./client";

export const pricingApi = {
  get: () => apiFetch<SubscriptionPricingResponse>("/api/admin/subscriptions/pricing"),

  updateBasePrice: (basePriceKrw: number) =>
    apiFetch<SubscriptionPricingResponse>("/api/admin/subscriptions/pricing/base-price", {
      method: "PUT",
      body: JSON.stringify({ basePriceKrw }),
    }),

  setPromotion: (promoPriceKrw: number, promoStartsAt: string, promoEndsAt: string) =>
    apiFetch<SubscriptionPricingResponse>("/api/admin/subscriptions/pricing/promotion", {
      method: "PUT",
      body: JSON.stringify({ promoPriceKrw, promoStartsAt, promoEndsAt }),
    }),

  clearPromotion: () =>
    apiFetch<SubscriptionPricingResponse>("/api/admin/subscriptions/pricing/promotion", {
      method: "DELETE",
    }),
};
