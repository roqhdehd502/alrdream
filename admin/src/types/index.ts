export type MemberRole = "USER" | "ADMIN";
export type MemberPlan = "FREE" | "PRO";
export type AuthProvider = "LOCAL" | "GOOGLE" | "APPLE";
export type SurveyKey = "PLANNING_HAS_IDEA" | "PLANNING_EXPLORING" | "DESIGN";
export type QuestionType = "SINGLE_CHOICE" | "MULTI_CHOICE" | "SHORT_TEXT" | "LONG_TEXT" | "SCALE";
export type AiTargetType = "PLANNING" | "ANALYSIS" | "DESIGN";
export type SubscriptionStatus = "ACTIVE" | "PAST_DUE" | "CANCELED";
export type PaymentStatus = "PAID" | "FAILED";

export interface ErrorResponse {
  code: string;
  message: string;
}

export interface TokenResponse {
  accessToken: string;
  refreshToken: string;
}

export interface MemberResponse {
  id: string;
  email: string;
  role: MemberRole;
  plan: MemberPlan;
}

export interface PagedModel<T> {
  content: T[];
  page: {
    size: number;
    number: number;
    totalElements: number;
    totalPages: number;
  };
}

export interface QuestionOption {
  key: string;
  label: string;
}

export interface Question {
  id: string;
  promptKey: string;
  type: QuestionType;
  question: string;
  required: boolean;
  options: QuestionOption[];
  allowUnknown: boolean;
}

export interface SurveyDefinitionResponse {
  id: string;
  surveyKey: SurveyKey;
  version: number;
  title: string;
  questions: Question[];
  createdAt: string;
}

export interface PromptTemplateResponse {
  id: string;
  promptType: AiTargetType;
  version: number;
  toolName: string;
  toolDescription: string;
  systemPrompt: string;
  schemaJson: string;
  createdAt: string;
}

export interface MemberAdminResponse {
  id: string;
  email: string;
  name: string | null;
  provider: AuthProvider;
  role: MemberRole;
  plan: MemberPlan;
  proExpiresAt: string | null;
  banned: boolean;
  permanentBan: boolean;
  tempBanUntil: string | null;
  createdAt: string;
}

export interface WorkspaceResponse {
  id: string;
  name: string;
  status: string;
  createdAt: string;
  updatedAt: string;
}

export interface SubscriptionAdminResponse {
  id: string;
  userId: string;
  userEmail: string | null;
  plan: MemberPlan;
  status: SubscriptionStatus;
  nextBillingAt: string | null;
  startedAt: string;
}

export interface SubscriptionSummaryResponse {
  activeCount: number;
  pastDueCount: number;
  canceledCount: number;
}

export interface DashboardSummaryResponse {
  totalMembers: number;
  freeMembers: number;
  proMembers: number;
  generationsThisMonth: number;
  paymentsSucceededThisMonth: number;
  paymentsFailedThisMonth: number;
}

export interface FreeTierLimitResponse {
  freeMonthlyLimit: number;
  proMonthlyLimit: number;
  updatedAt: string;
}

export interface SubscriptionPricingResponse {
  basePriceKrw: number;
  promoPriceKrw: number | null;
  promoStartsAt: string | null;
  promoEndsAt: string | null;
  promoActive: boolean;
  effectivePriceKrw: number;
  updatedAt: string;
}

export interface PaymentAdminResponse {
  id: string;
  subscriptionId: string;
  userId: string;
  userEmail: string | null;
  amount: number;
  status: PaymentStatus;
  paidAt: string | null;
  createdAt: string;
}

export interface CouponResponse {
  id: string;
  code: string;
  benefitDays: number;
  maxRedemptions: number | null;
  redemptionCount: number;
  expiresAt: string | null;
  active: boolean;
  createdAt: string;
}

export interface CouponRedemptionAdminResponse {
  id: string;
  couponCode: string;
  userId: string;
  userEmail: string | null;
  redeemedAt: string;
  grantedUntil: string;
}
