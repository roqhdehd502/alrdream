import { apiClient } from "./client";
import type { PaymentHistoryResponse, PricingResponse, SubscriptionResponse } from "../types";

export const subscriptionApi = {
  getCurrent: () => apiClient.get<SubscriptionResponse>("/api/subscriptions/me"),
  subscribe: (billingKeyId: string) => apiClient.post<SubscriptionResponse>("/api/subscriptions", { billingKeyId }),
  cancel: () => apiClient.delete<SubscriptionResponse>("/api/subscriptions/me"),
  getPayments: () => apiClient.get<PaymentHistoryResponse[]>("/api/subscriptions/me/payments"),
  getPricing: () => apiClient.get<PricingResponse>("/api/subscriptions/pricing"),
};
