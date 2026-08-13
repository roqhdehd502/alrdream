import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { couponsApi } from "../api/coupons";
import { ApiError } from "../api/client";
import { Pagination } from "../components/Pagination";
import { PageHeader } from "../components/PageHeader";
import { EmptyState, ErrorAlert, Loading } from "../components/Feedback";
import type { CouponRedemptionAdminResponse } from "../types";

function formatDate(value: string) {
  return new Date(value).toLocaleString("ko-KR");
}

export function CouponRedemptionsPage() {
  const [page, setPage] = useState(0);
  const [redemptions, setRedemptions] = useState<CouponRedemptionAdminResponse[] | null>(null);
  const [pageInfo, setPageInfo] = useState({ totalPages: 0, totalElements: 0 });
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    setRedemptions(null);
    couponsApi
      .redemptions(page)
      .then((res) => {
        if (cancelled) return;
        setRedemptions(res.content);
        setPageInfo({ totalPages: res.page.totalPages, totalElements: res.page.totalElements });
      })
      .catch((e) => {
        if (!cancelled) setError(e instanceof ApiError ? e.message : String(e));
      });
    return () => {
      cancelled = true;
    };
  }, [page]);

  return (
    <div>
      <PageHeader title="유저 쿠폰 사용 현황" description="사용자별 쿠폰 사용 이력을 조회합니다." />

      <ErrorAlert message={error} />

      {redemptions === null ? (
        <Loading />
      ) : redemptions.length === 0 ? (
        <EmptyState label="쿠폰 사용 이력이 없습니다." />
      ) : (
        <>
          <div className="table-scroll">
            <table>
              <thead>
                <tr>
                  <th>사용자</th>
                  <th>쿠폰 코드</th>
                  <th>사용일</th>
                  <th>부여된 Pro 만료일</th>
                </tr>
              </thead>
              <tbody>
                {redemptions.map((r) => (
                  <tr key={r.id}>
                    <td>
                      {r.userId ? <Link to={`/users/${r.userId}`}>{r.userEmail ?? r.userId}</Link> : (r.userEmail ?? "-")}
                    </td>
                    <td>{r.couponCode}</td>
                    <td>{formatDate(r.redeemedAt)}</td>
                    <td>{formatDate(r.grantedUntil)}</td>
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
