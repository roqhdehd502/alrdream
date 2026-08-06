import type { ReactNode } from "react";
import { Navigate } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";
import { Loading } from "./Feedback";

export function ProtectedRoute({ children }: { children: ReactNode }) {
  const { status } = useAuth();

  if (status === "loading") {
    return (
      <div className="login-shell">
        <Loading />
      </div>
    );
  }

  if (status === "unauthenticated") {
    return <Navigate to="/login" replace />;
  }

  return <>{children}</>;
}
