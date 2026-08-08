import { useEffect, useState } from "react";
import { subscriptionsApi } from "../api/subscriptions";
import { settingsApi } from "../api/settings";
import { ApiError } from "../api/client";
import { Pagination } from "../components/Pagination";
import { PageHeader } from "../components/PageHeader";
import { EmptyState, ErrorAlert, Loading } from "../components/Feedback";
import type { SubscriptionAdminResponse, SubscriptionStatus, SubscriptionSummaryResponse } from "../types";

const STATUS_LABEL: Record<SubscriptionStatus, string> = {
  ACTIVE: "정상 결제 중",
  PAST_DUE: "결제 대기/실패",
  CANCELED: "해지됨",
};

const STATUS_BADGE: Record<SubscriptionStatus, string> = {
  ACTIVE: "badge-success",
  PAST_DUE: "badge-warning",
  CANCELED: "badge-danger",
};

function formatDate(value: string | null) {
  if (!value) return "-";
  return new Date(value).toLocaleString("ko-KR");
}

export function DashboardPage() {
  const [summary, setSummary] = useState<SubscriptionSummaryResponse | null>(null);
  const [freeTierLimit, setFreeTierLimit] = useState<number | null>(null);
  const [limitInput, setLimitInput] = useState("");
  const [limitSaving, setLimitSaving] = useState(false);
  const [limitMessage, setLimitMessage] = useState<string | null>(null);

  const [statusFilter, setStatusFilter] = useState<SubscriptionStatus | "">("");
  const [page, setPage] = useState(0);
  const [subscriptions, setSubscriptions] = useState<SubscriptionAdminResponse[] | null>(null);
  const [pageInfo, setPageInfo] = useState({ totalPages: 0, totalElements: 0 });
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    subscriptionsApi.summary().then(setSummary).catch((e) => setError(e instanceof ApiError ? e.message : String(e)));
    settingsApi
      .getFreeTierLimit()
      .then((res) => {
        setFreeTierLimit(res.monthlyLimit);
        setLimitInput(String(res.monthlyLimit));
      })
      .catch((e) => setError(e instanceof ApiError ? e.message : String(e)));
  }, []);

  useEffect(() => {
    setSubscriptions(null);
    subscriptionsApi
      .list(statusFilter || undefined, page)
      .then((res) => {
        setSubscriptions(res.content);
        setPageInfo({ totalPages: res.page.totalPages, totalElements: res.page.totalElements });
      })
      .catch((e) => setError(e instanceof ApiError ? e.message : String(e)));
  }, [statusFilter, page]);

  const saveLimit = async () => {
    if (freeTierLimit === null) {
      setLimitMessage("현재 값을 불러오지 못해 저장할 수 없습니다. 새로고침 후 다시 시도해주세요.");
      return;
    }
    if (limitInput.trim() === "") {
      setLimitMessage("0 이상의 정수를 입력해주세요.");
      return;
    }
    const value = Number(limitInput);
    if (!Number.isInteger(value) || value < 0) {
      setLimitMessage("0 이상의 정수를 입력해주세요.");
      return;
    }
    setLimitSaving(true);
    setLimitMessage(null);
    try {
      const res = await settingsApi.updateFreeTierLimit(value);
      setFreeTierLimit(res.monthlyLimit);
      setLimitMessage("저장되었습니다.");
    } catch (e) {
      setLimitMessage(e instanceof ApiError ? e.message : "저장에 실패했습니다.");
    } finally {
      setLimitSaving(false);
    }
  };

  return (
    <div>
      <PageHeader title="구독/사용량 대시보드" description="Pro 구독 현황과 FREE 플랜 월별 생성 횟수 한도를 관리합니다." />

      <ErrorAlert message={error} />

      <div className="card-grid">
        <div className="stat-card">
          <div className="stat-label">정상 결제 중</div>
          <div className="stat-value">{summary?.activeCount ?? "-"}</div>
        </div>
        <div className="stat-card">
          <div className="stat-label">결제 대기/실패</div>
          <div className="stat-value">{summary?.pastDueCount ?? "-"}</div>
        </div>
        <div className="stat-card">
          <div className="stat-label">해지됨</div>
          <div className="stat-value">{summary?.canceledCount ?? "-"}</div>
        </div>
      </div>

      <div className="card">
        <h3 style={{ marginTop: 0, marginBottom: 4 }}>FREE 플랜 월별 생성 횟수 한도</h3>
        <p style={{ color: "var(--color-text-muted)", fontSize: 13, marginTop: 0, marginBottom: 16 }}>
          현재 값: <strong>{freeTierLimit ?? "-"}회 / 월</strong>. 변경 시 이후 새로 시작되는 달부터 적용됩니다.
        </p>
        <div className="form-row" style={{ alignItems: "flex-end" }}>
          <div className="form-field" style={{ maxWidth: 160 }}>
            <label htmlFor="limit">월별 한도(회)</label>
            <input
              id="limit"
              type="number"
              min={0}
              value={limitInput}
              onChange={(e) => setLimitInput(e.target.value)}
            />
          </div>
          <button
            type="button"
            className="btn btn-primary"
            onClick={saveLimit}
            disabled={limitSaving || freeTierLimit === null}
          >
            {limitSaving ? "저장 중..." : "저장"}
          </button>
        </div>
        {limitMessage && <div className="alert alert-muted">{limitMessage}</div>}
      </div>

      <div className="toolbar">
        <select
          value={statusFilter}
          onChange={(e) => {
            setStatusFilter(e.target.value as SubscriptionStatus | "");
            setPage(0);
          }}
        >
          <option value="">전체 상태</option>
          <option value="ACTIVE">정상 결제 중</option>
          <option value="PAST_DUE">결제 대기/실패</option>
          <option value="CANCELED">해지됨</option>
        </select>
      </div>

      {subscriptions === null ? (
        <Loading />
      ) : subscriptions.length === 0 ? (
        <EmptyState label="구독 내역이 없습니다." />
      ) : (
        <>
          <div className="table-scroll">
            <table>
              <thead>
                <tr>
                  <th>사용자</th>
                  <th>요금제</th>
                  <th>상태</th>
                  <th>다음 결제일</th>
                  <th>구독 시작일</th>
                </tr>
              </thead>
              <tbody>
                {subscriptions.map((sub) => (
                  <tr key={sub.id}>
                    <td>{sub.userEmail ?? sub.userId}</td>
                    <td>{sub.plan}</td>
                    <td>
                      <span className={`badge ${STATUS_BADGE[sub.status]}`}>{STATUS_LABEL[sub.status]}</span>
                    </td>
                    <td>{formatDate(sub.nextBillingAt)}</td>
                    <td>{formatDate(sub.startedAt)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <Pagination
            page={page}
            totalPages={pageInfo.totalPages}
            totalElements={pageInfo.totalElements}
            onChange={setPage}
          />
        </>
      )}
    </div>
  );
}
