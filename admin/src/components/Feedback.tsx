export function Loading({ label = "불러오는 중..." }: { label?: string }) {
  return (
    <div className="loading-row">
      <span className="spinner" />
      {label}
    </div>
  );
}

export function EmptyState({ label }: { label: string }) {
  return <div className="empty-state">{label}</div>;
}

export function ErrorAlert({ message }: { message: string | null }) {
  if (!message) return null;
  return <div className="alert alert-error">{message}</div>;
}
