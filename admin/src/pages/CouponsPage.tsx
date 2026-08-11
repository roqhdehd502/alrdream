import { useEffect, useState } from "react";
import { couponsApi } from "../api/coupons";
import { ApiError } from "../api/client";
import { Pagination } from "../components/Pagination";
import { PageHeader } from "../components/PageHeader";
import { EmptyState, ErrorAlert, Loading } from "../components/Feedback";
import type { CouponResponse } from "../types";

function formatDate(value: string | null) {
  if (!value) return "무기한";
  return new Date(value).toLocaleString("ko-KR");
}

function fromDatetimeLocalValue(value: string): string {
  return new Date(value).toISOString();
}

export function CouponsPage() {
  const [page, setPage] = useState(0);
  const [coupons, setCoupons] = useState<CouponResponse[] | null>(null);
  const [pageInfo, setPageInfo] = useState({ totalPages: 0, totalElements: 0 });
  const [error, setError] = useState<string | null>(null);

  const [code, setCode] = useState("");
  const [benefitDays, setBenefitDays] = useState("");
  const [maxRedemptions, setMaxRedemptions] = useState("");
  const [expiresAt, setExpiresAt] = useState("");
  const [creating, setCreating] = useState(false);
  const [createMessage, setCreateMessage] = useState<string | null>(null);
  const [busyCouponId, setBusyCouponId] = useState<string | null>(null);

  const load = () => {
    setCoupons(null);
    couponsApi
      .list(page)
      .then((res) => {
        setCoupons(res.content);
        setPageInfo({ totalPages: res.page.totalPages, totalElements: res.page.totalElements });
      })
      .catch((e) => setError(e instanceof ApiError ? e.message : String(e)));
  };

  useEffect(load, [page]);

  const createCoupon = async () => {
    if (!code.trim()) {
      setCreateMessage("쿠폰 코드를 입력해주세요.");
      return;
    }
    const days = Number(benefitDays);
    if (!Number.isInteger(days) || days < 1) {
      setCreateMessage("지급 일수는 1 이상의 정수를 입력해주세요.");
      return;
    }
    setCreating(true);
    setCreateMessage(null);
    try {
      await couponsApi.create({
        code: code.trim(),
        benefitDays: days,
        maxRedemptions: maxRedemptions.trim() ? Number(maxRedemptions) : undefined,
        expiresAt: expiresAt ? fromDatetimeLocalValue(expiresAt) : undefined,
      });
      setCode("");
      setBenefitDays("");
      setMaxRedemptions("");
      setExpiresAt("");
      setCreateMessage("생성되었습니다.");
      setPage(0);
      load();
    } catch (e) {
      setCreateMessage(e instanceof ApiError ? e.message : "생성에 실패했습니다.");
    } finally {
      setCreating(false);
    }
  };

  const deactivate = async (couponId: string) => {
    setBusyCouponId(couponId);
    try {
      await couponsApi.deactivate(couponId);
      load();
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "비활성화에 실패했습니다.");
    } finally {
      setBusyCouponId(null);
    }
  };

  return (
    <div>
      <PageHeader title="쿠폰 코드 관리" description="이벤트로 공개할 쿠폰 코드를 생성하고 관리합니다." />

      <ErrorAlert message={error} />

      <div className="card">
        <h3 style={{ marginTop: 0 }}>새 쿠폰 코드</h3>
        <div className="form-row">
          <div className="form-field">
            <label htmlFor="code">코드</label>
            <input id="code" type="text" value={code} onChange={(e) => setCode(e.target.value)} placeholder="예: SUMMER2026" />
          </div>
          <div className="form-field" style={{ maxWidth: 140 }}>
            <label htmlFor="benefitDays">지급 일수</label>
            <input
              id="benefitDays"
              type="number"
              min={1}
              value={benefitDays}
              onChange={(e) => setBenefitDays(e.target.value)}
            />
          </div>
          <div className="form-field" style={{ maxWidth: 160 }}>
            <label htmlFor="maxRedemptions">최대 사용 횟수(선택)</label>
            <input
              id="maxRedemptions"
              type="number"
              min={1}
              value={maxRedemptions}
              onChange={(e) => setMaxRedemptions(e.target.value)}
              placeholder="무제한"
            />
          </div>
          <div className="form-field">
            <label htmlFor="expiresAt">코드 사용 기한(선택)</label>
            <input
              id="expiresAt"
              type="datetime-local"
              value={expiresAt}
              onChange={(e) => setExpiresAt(e.target.value)}
            />
          </div>
        </div>
        <button type="button" className="btn btn-primary" onClick={createCoupon} disabled={creating}>
          {creating ? "생성 중..." : "쿠폰 생성"}
        </button>
        {createMessage && (
          <div className="alert alert-muted" role="status">
            {createMessage}
          </div>
        )}
      </div>

      <h3 className="section-heading">쿠폰 코드 목록</h3>
      {coupons === null ? (
        <Loading />
      ) : coupons.length === 0 ? (
        <EmptyState label="생성된 쿠폰이 없습니다." />
      ) : (
        <>
          <div className="table-scroll">
            <table>
              <thead>
                <tr>
                  <th>코드</th>
                  <th>지급 일수</th>
                  <th>사용/한도</th>
                  <th>사용 기한</th>
                  <th>상태</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {coupons.map((c) => (
                  <tr key={c.id}>
                    <td>{c.code}</td>
                    <td>{c.benefitDays}일</td>
                    <td>
                      {c.redemptionCount} / {c.maxRedemptions ?? "무제한"}
                    </td>
                    <td>{formatDate(c.expiresAt)}</td>
                    <td>
                      <span className={`badge ${c.active ? "badge-success" : "badge-danger"}`}>
                        {c.active ? "활성" : "비활성"}
                      </span>
                    </td>
                    <td>
                      {c.active && (
                        <button
                          type="button"
                          className="btn btn-danger"
                          onClick={() => deactivate(c.id)}
                          disabled={busyCouponId === c.id}
                        >
                          {busyCouponId === c.id ? "처리 중..." : "비활성화"}
                        </button>
                      )}
                    </td>
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
