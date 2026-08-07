import { Badge } from "../ui/Badge";
import type { VersionStatus } from "../../types";

const LABEL: Record<VersionStatus, string> = {
  GENERATING: "생성 중",
  COMPLETED: "완료",
  FAILED: "실패",
};

const TONE: Record<VersionStatus, "primary" | "success" | "danger"> = {
  GENERATING: "primary",
  COMPLETED: "success",
  FAILED: "danger",
};

export function StatusBadge({ status }: { status: VersionStatus }) {
  return <Badge label={LABEL[status]} tone={TONE[status]} />;
}
