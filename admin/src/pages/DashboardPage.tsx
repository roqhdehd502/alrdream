import { useEffect, useState } from "react";
import { dashboardApi } from "../api/dashboard";
import { ApiError } from "../api/client";
import { PageHeader } from "../components/PageHeader";
import { ErrorAlert } from "../components/Feedback";
import { StatusBarChart } from "../components/charts/StatusBarChart";
import type { DashboardSummaryResponse } from "../types";

export function DashboardPage() {
  const [dashboardSummary, setDashboardSummary] = useState<DashboardSummaryResponse | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    dashboardApi
      .summary()
      .then(setDashboardSummary)
      .catch((e) => setError(e instanceof ApiError ? e.message : String(e)));
  }, []);

  return (
    <div>
      <PageHeader title="대시보드" description="가입자/생성량/결제 현황을 한눈에 확인합니다." />

      <ErrorAlert message={error} />

      <div className="card-grid">
        <div className="stat-card">
          <div className="stat-label">전체 가입자</div>
          <div className="stat-value">{dashboardSummary?.totalMembers ?? "-"}</div>
        </div>
        <div className="stat-card">
          <div className="stat-label">이번 달 AI 생성</div>
          <div className="stat-value">{dashboardSummary?.generationsThisMonth ?? "-"}</div>
        </div>
      </div>

      <div className="card-grid card-grid-charts">
        <div className="card">
          <h3 style={{ marginTop: 0 }}>가입자 플랜 분포</h3>
          {dashboardSummary ? (
            <StatusBarChart
              data={[
                { label: "FREE", value: dashboardSummary.freeMembers, color: "var(--color-text-faint)" },
                { label: "PRO", value: dashboardSummary.proMembers, color: "var(--color-primary)" },
              ]}
            />
          ) : (
            <div className="loading-row">불러오는 중...</div>
          )}
        </div>
        <div className="card">
          <h3 style={{ marginTop: 0 }}>이번 달 결제 성공/실패</h3>
          {dashboardSummary ? (
            <StatusBarChart
              data={[
                { label: "성공", value: dashboardSummary.paymentsSucceededThisMonth, color: "var(--color-success)" },
                { label: "실패", value: dashboardSummary.paymentsFailedThisMonth, color: "var(--color-danger)" },
              ]}
            />
          ) : (
            <div className="loading-row">불러오는 중...</div>
          )}
        </div>
      </div>
    </div>
  );
}
