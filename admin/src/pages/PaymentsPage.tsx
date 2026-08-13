import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { paymentsApi } from "../api/payments";
import { ApiError } from "../api/client";
import { Pagination } from "../components/Pagination";
import { PageHeader } from "../components/PageHeader";
import { EmptyState, ErrorAlert, Loading } from "../components/Feedback";
import type { PaymentAdminResponse, PaymentStatus } from "../types";

const STATUS_LABEL: Record<PaymentStatus, string> = {
  PAID: "결제 성공",
  FAILED: "결제 실패",
};

const STATUS_BADGE: Record<PaymentStatus, string> = {
  PAID: "badge-success",
  FAILED: "badge-danger",
};

function formatDate(value: string | null) {
  if (!value) return "-";
  return new Date(value).toLocaleString("ko-KR");
}

export function PaymentsPage() {
  const [statusFilter, setStatusFilter] = useState<PaymentStatus | "">("");
  const [page, setPage] = useState(0);
  const [payments, setPayments] = useState<PaymentAdminResponse[] | null>(null);
  const [pageInfo, setPageInfo] = useState({ totalPages: 0, totalElements: 0 });
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    setPayments(null);
    paymentsApi
      .list(statusFilter || undefined, page)
      .then((res) => {
        if (cancelled) return;
        setPayments(res.content);
        setPageInfo({ totalPages: res.page.totalPages, totalElements: res.page.totalElements });
      })
      .catch((e) => {
        if (!cancelled) setError(e instanceof ApiError ? e.message : String(e));
      });
    return () => {
      cancelled = true;
    };
  }, [statusFilter, page]);

  return (
    <div>
      <PageHeader title="결제 관리" description="개별 결제 시도 이력을 조회합니다." />

      <ErrorAlert message={error} />

      <div className="toolbar">
        <select
          value={statusFilter}
          onChange={(e) => {
            setStatusFilter(e.target.value as PaymentStatus | "");
            setPage(0);
          }}
        >
          <option value="">전체 상태</option>
          <option value="PAID">결제 성공</option>
          <option value="FAILED">결제 실패</option>
        </select>
      </div>

      {payments === null ? (
        <Loading />
      ) : payments.length === 0 ? (
        <EmptyState label="결제 내역이 없습니다." />
      ) : (
        <>
          <div className="table-scroll">
            <table>
              <thead>
                <tr>
                  <th>사용자</th>
                  <th>금액</th>
                  <th>상태</th>
                  <th>결제일</th>
                  <th>기록일</th>
                </tr>
              </thead>
              <tbody>
                {payments.map((p) => (
                  <tr key={p.id}>
                    <td>
                      {p.userId ? (
                        <Link to={`/users/${p.userId}`}>{p.userEmail ?? p.userId}</Link>
                      ) : (
                        (p.userEmail ?? "-")
                      )}
                    </td>
                    <td>{p.amount.toLocaleString("ko-KR")}원</td>
                    <td>
                      <span className={`badge ${STATUS_BADGE[p.status]}`}>{STATUS_LABEL[p.status]}</span>
                    </td>
                    <td>{formatDate(p.paidAt)}</td>
                    <td>{formatDate(p.createdAt)}</td>
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
