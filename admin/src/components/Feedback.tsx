import type { ReactNode } from "react";
import { InboxIcon } from "./icons";

export function Loading({ label = "불러오는 중..." }: { label?: string }) {
  return (
    <div className="loading-row">
      <span className="spinner" />
      {label}
    </div>
  );
}

export function EmptyState({ label, icon }: { label: string; icon?: ReactNode }) {
  return (
    <div className="empty-state">
      <span className="empty-state-icon">{icon ?? <InboxIcon size={20} />}</span>
      {label}
    </div>
  );
}

export function ErrorAlert({ message }: { message: string | null }) {
  if (!message) return null;
  return (
    <div className="alert alert-error" role="alert">
      {message}
    </div>
  );
}
