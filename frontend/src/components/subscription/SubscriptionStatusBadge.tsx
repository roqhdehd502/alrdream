import { Badge } from "../ui/Badge";
import type { SubscriptionStatus } from "../../types";

const LABEL: Record<SubscriptionStatus, string> = {
  ACTIVE: "구독 중",
  PAST_DUE: "결제 확인 중",
  CANCELED: "해지됨",
};

const TONE: Record<SubscriptionStatus, "success" | "warning" | "neutral"> = {
  ACTIVE: "success",
  PAST_DUE: "warning",
  CANCELED: "neutral",
};

export function SubscriptionStatusBadge({ status }: { status: SubscriptionStatus }) {
  return <Badge label={LABEL[status]} tone={TONE[status]} />;
}
