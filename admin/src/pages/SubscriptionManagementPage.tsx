import { useEffect, useState } from "react";
import { subscriptionsApi } from "../api/subscriptions";
import { pricingApi } from "../api/pricing";
import { ApiError } from "../api/client";
import { Pagination } from "../components/Pagination";
import { PageHeader } from "../components/PageHeader";
import { EmptyState, ErrorAlert, Loading } from "../components/Feedback";
import { StatusBarChart } from "../components/charts/StatusBarChart";
import type {
  SubscriptionAdminResponse,
  SubscriptionPricingResponse,
  SubscriptionStatus,
  SubscriptionSummaryResponse,
} from "../types";

// <input type="datetime-local">는 로컬 시각(초/타임존 없이)을 준다 — 브라우저의 로컬 타임존을 그대로
// 신뢰해 Date로 변환한다(운영자가 관리자 콘솔을 쓰는 타임존 = 프로모션을 적용할 타임존).
function fromDatetimeLocalValue(value: string): string {
  return new Date(value).toISOString();
}

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

export function SubscriptionManagementPage() {
  const [summary, setSummary] = useState<SubscriptionSummaryResponse | null>(null);

  const [pricing, setPricing] = useState<SubscriptionPricingResponse | null>(null);
  const [basePriceInput, setBasePriceInput] = useState("");
  const [basePriceSaving, setBasePriceSaving] = useState(false);
  const [promoPriceInput, setPromoPriceInput] = useState("");
  const [promoStartInput, setPromoStartInput] = useState("");
  const [promoEndInput, setPromoEndInput] = useState("");
  const [promoSaving, setPromoSaving] = useState(false);
  const [pricingMessage, setPricingMessage] = useState<string | null>(null);

  const [statusFilter, setStatusFilter] = useState<SubscriptionStatus | "">("");
  const [page, setPage] = useState(0);
  const [subscriptions, setSubscriptions] = useState<SubscriptionAdminResponse[] | null>(null);
  const [pageInfo, setPageInfo] = useState({ totalPages: 0, totalElements: 0 });
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    subscriptionsApi.summary().then(setSummary).catch((e) => setError(e instanceof ApiError ? e.message : String(e)));
    loadPricing();
  }, []);

  const loadPricing = () => {
    pricingApi
      .get()
      .then((res) => {
        setPricing(res);
        setBasePriceInput(String(res.basePriceKrw));
      })
      .catch((e) => setError(e instanceof ApiError ? e.message : String(e)));
  };

  useEffect(() => {
    let cancelled = false;
    setSubscriptions(null);
    subscriptionsApi
      .list(statusFilter || undefined, page)
      .then((res) => {
        if (cancelled) return;
        setSubscriptions(res.content);
        setPageInfo({ totalPages: res.page.totalPages, totalElements: res.page.totalElements });
      })
      .catch((e) => {
        if (!cancelled) setError(e instanceof ApiError ? e.message : String(e));
      });
    return () => {
      cancelled = true;
    };
  }, [statusFilter, page]);

  const saveBasePrice = async () => {
    if (basePriceInput.trim() === "") {
      setPricingMessage("0 이상의 정수를 입력해주세요.");
      return;
    }
    const value = Number(basePriceInput);
    if (!Number.isInteger(value) || value < 0) {
      setPricingMessage("0 이상의 정수를 입력해주세요.");
      return;
    }
    setBasePriceSaving(true);
    setPricingMessage(null);
    try {
      const res = await pricingApi.updateBasePrice(value);
      setPricing(res);
      setPricingMessage("기본 요금이 저장되었습니다.");
    } catch (e) {
      setPricingMessage(e instanceof ApiError ? e.message : "저장에 실패했습니다.");
    } finally {
      setBasePriceSaving(false);
    }
  };

  const savePromotion = async () => {
    const value = Number(promoPriceInput);
    if (promoPriceInput.trim() === "" || !Number.isInteger(value) || value < 0) {
      setPricingMessage("프로모션 가격은 0 이상의 정수를 입력해주세요.");
      return;
    }
    if (!promoStartInput || !promoEndInput) {
      setPricingMessage("프로모션 시작/종료 일시를 모두 입력해주세요.");
      return;
    }
    setPromoSaving(true);
    setPricingMessage(null);
    try {
      const res = await pricingApi.setPromotion(
        value,
        fromDatetimeLocalValue(promoStartInput),
        fromDatetimeLocalValue(promoEndInput),
      );
      setPricing(res);
      setPricingMessage("프로모션이 설정되었습니다.");
    } catch (e) {
      setPricingMessage(e instanceof ApiError ? e.message : "설정에 실패했습니다.");
    } finally {
      setPromoSaving(false);
    }
  };

  const clearPromotion = async () => {
    setPromoSaving(true);
    setPricingMessage(null);
    try {
      const res = await pricingApi.clearPromotion();
      setPricing(res);
      setPromoPriceInput("");
      setPromoStartInput("");
      setPromoEndInput("");
      setPricingMessage("프로모션이 해제되었습니다.");
    } catch (e) {
      setPricingMessage(e instanceof ApiError ? e.message : "해제에 실패했습니다.");
    } finally {
      setPromoSaving(false);
    }
  };

  return (
    <div>
      <PageHeader title="구독 관리" description="Pro 구독 현황과 가격/프로모션을 관리합니다." />

      <ErrorAlert message={error} />

      <div className="card">
        <h3 style={{ marginTop: 0 }}>구독 상태 분포</h3>
        {summary ? (
          <StatusBarChart
            data={[
              { label: STATUS_LABEL.ACTIVE, value: summary.activeCount, color: "var(--color-success)" },
              { label: STATUS_LABEL.PAST_DUE, value: summary.pastDueCount, color: "var(--color-warning)" },
              { label: STATUS_LABEL.CANCELED, value: summary.canceledCount, color: "var(--color-danger)" },
            ]}
          />
        ) : (
          <div className="loading-row">불러오는 중...</div>
        )}
      </div>

      <div className="card">
        <h3 style={{ marginTop: 0, marginBottom: 4 }}>Pro 구독 가격 관리</h3>
        <p style={{ color: "var(--color-text-muted)", fontSize: 13, marginTop: 0, marginBottom: 16 }}>
          현재 청구가:{" "}
          <strong>
            {pricing?.effectivePriceKrw.toLocaleString("ko-KR") ?? "-"}원
            {pricing?.promoActive && (
              <span style={{ color: "var(--color-text-muted)", fontWeight: 400 }}>
                {" "}
                (프로모션 진행 중 — 기본가 {pricing.basePriceKrw.toLocaleString("ko-KR")}원)
              </span>
            )}
          </strong>
          . 변경/설정 시 다음 청구(신규 구독·기존 구독 다음 달 갱신 모두)부터 적용되며, 이미 예약된
          결제 건은 소급 변경되지 않습니다.
        </p>

        <div className="form-row" style={{ alignItems: "flex-end" }}>
          <div className="form-field" style={{ maxWidth: 160 }}>
            <label htmlFor="basePrice">기본 월 요금(원)</label>
            <input
              id="basePrice"
              type="number"
              min={0}
              value={basePriceInput}
              onChange={(e) => setBasePriceInput(e.target.value)}
            />
          </div>
          <button type="button" className="btn btn-primary" onClick={saveBasePrice} disabled={basePriceSaving}>
            {basePriceSaving ? "저장 중..." : "기본가 저장"}
          </button>
        </div>

        <h4 style={{ marginBottom: 4, marginTop: 20 }}>프로모션</h4>
        {pricing?.promoActive ? (
          <>
            <p style={{ color: "var(--color-text-muted)", fontSize: 13, marginTop: 0, marginBottom: 12 }}>
              <span className="badge badge-success">진행 중</span>{" "}
              {pricing.promoPriceKrw?.toLocaleString("ko-KR")}원 · 종료:{" "}
              {pricing.promoEndsAt ? new Date(pricing.promoEndsAt).toLocaleString("ko-KR") : "-"}
            </p>
            <button type="button" className="btn btn-danger" onClick={clearPromotion} disabled={promoSaving}>
              {promoSaving ? "처리 중..." : "프로모션 조기 종료"}
            </button>
          </>
        ) : (
          <>
            <p style={{ color: "var(--color-text-muted)", fontSize: 13, marginTop: 0, marginBottom: 12 }}>
              현재 진행 중인 프로모션이 없습니다. 기간을 지정해 할인가를 설정할 수 있습니다.
            </p>
            <div className="form-row" style={{ alignItems: "flex-end", flexWrap: "wrap" }}>
              <div className="form-field" style={{ maxWidth: 140 }}>
                <label htmlFor="promoPrice">프로모션 가격(원)</label>
                <input
                  id="promoPrice"
                  type="number"
                  min={0}
                  value={promoPriceInput}
                  onChange={(e) => setPromoPriceInput(e.target.value)}
                />
              </div>
              <div className="form-field">
                <label htmlFor="promoStart">시작</label>
                <input
                  id="promoStart"
                  type="datetime-local"
                  value={promoStartInput}
                  onChange={(e) => setPromoStartInput(e.target.value)}
                />
              </div>
              <div className="form-field">
                <label htmlFor="promoEnd">종료</label>
                <input
                  id="promoEnd"
                  type="datetime-local"
                  value={promoEndInput}
                  onChange={(e) => setPromoEndInput(e.target.value)}
                />
              </div>
              <button type="button" className="btn btn-primary" onClick={savePromotion} disabled={promoSaving}>
                {promoSaving ? "저장 중..." : "프로모션 설정"}
              </button>
            </div>
          </>
        )}

        {pricingMessage && (
          <div className="alert alert-muted" role="status">
            {pricingMessage}
          </div>
        )}
      </div>

      <h3 className="section-heading">구독자 목록</h3>
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
