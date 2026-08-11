import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { membersApi } from "../api/members";
import { paymentsApi } from "../api/payments";
import { ApiError } from "../api/client";
import { Pagination } from "../components/Pagination";
import { PageHeader } from "../components/PageHeader";
import { EmptyState, ErrorAlert, Loading } from "../components/Feedback";
import type { MemberAdminResponse, PaymentAdminResponse, PaymentStatus, WorkspaceResponse } from "../types";

const PAYMENT_STATUS_LABEL: Record<PaymentStatus, string> = {
  PAID: "결제 성공",
  FAILED: "결제 실패",
};

const PAYMENT_STATUS_BADGE: Record<PaymentStatus, string> = {
  PAID: "badge-success",
  FAILED: "badge-danger",
};

function formatDate(value: string | null) {
  if (!value) return "-";
  return new Date(value).toLocaleString("ko-KR");
}

function fromDatetimeLocalValue(value: string): string {
  return new Date(value).toISOString();
}

export function UserDetailPage() {
  const { userId } = useParams<{ userId: string }>();
  const [member, setMember] = useState<MemberAdminResponse | null>(null);
  const [workspaces, setWorkspaces] = useState<WorkspaceResponse[] | null>(null);
  const [page, setPage] = useState(0);
  const [pageInfo, setPageInfo] = useState({ totalPages: 0, totalElements: 0 });
  const [payments, setPayments] = useState<PaymentAdminResponse[] | null>(null);
  const [paymentPage, setPaymentPage] = useState(0);
  const [paymentPageInfo, setPaymentPageInfo] = useState({ totalPages: 0, totalElements: 0 });
  const [error, setError] = useState<string | null>(null);

  const [grantDays, setGrantDays] = useState("");
  const [banUntil, setBanUntil] = useState("");
  const [actionBusy, setActionBusy] = useState<string | null>(null);
  const [actionMessage, setActionMessage] = useState<string | null>(null);

  const loadMember = () => {
    if (!userId) return;
    membersApi
      .get(userId)
      .then(setMember)
      .catch((e) => setError(e instanceof ApiError ? e.message : String(e)));
  };

  useEffect(() => {
    if (!userId) return;
    let cancelled = false;
    setMember(null);
    membersApi
      .get(userId)
      .then((res) => {
        if (!cancelled) setMember(res);
      })
      .catch((e) => {
        if (!cancelled) setError(e instanceof ApiError ? e.message : String(e));
      });
    return () => {
      cancelled = true;
    };
  }, [userId]);

  const grantPro = async () => {
    if (!userId) return;
    const days = Number(grantDays);
    if (!Number.isInteger(days) || days < 1) {
      setActionMessage("일수는 1 이상의 정수를 입력해주세요.");
      return;
    }
    setActionBusy("grant");
    setActionMessage(null);
    try {
      await membersApi.bulkGrantPro([userId], days);
      setGrantDays("");
      setActionMessage(`Pro ${days}일을 지급/연장했습니다.`);
      loadMember();
    } catch (e) {
      setActionMessage(e instanceof ApiError ? e.message : "지급에 실패했습니다.");
    } finally {
      setActionBusy(null);
    }
  };

  const downgrade = async () => {
    if (!userId) return;
    setActionBusy("downgrade");
    setActionMessage(null);
    try {
      setMember(await membersApi.downgrade(userId));
      setActionMessage("Free로 전환했습니다.");
    } catch (e) {
      setActionMessage(e instanceof ApiError ? e.message : "전환에 실패했습니다.");
    } finally {
      setActionBusy(null);
    }
  };

  const ban = async (permanent: boolean) => {
    if (!userId) return;
    if (!permanent && !banUntil) {
      setActionMessage("일시 정지 해제 시각을 입력해주세요.");
      return;
    }
    setActionBusy("ban");
    setActionMessage(null);
    try {
      setMember(await membersApi.ban(userId, permanent, permanent ? undefined : fromDatetimeLocalValue(banUntil)));
      setBanUntil("");
      setActionMessage(permanent ? "영구 정지했습니다." : "일시 정지했습니다.");
    } catch (e) {
      setActionMessage(e instanceof ApiError ? e.message : "정지에 실패했습니다.");
    } finally {
      setActionBusy(null);
    }
  };

  const unban = async () => {
    if (!userId) return;
    setActionBusy("unban");
    setActionMessage(null);
    try {
      setMember(await membersApi.unban(userId));
      setActionMessage("정지를 해제했습니다.");
    } catch (e) {
      setActionMessage(e instanceof ApiError ? e.message : "해제에 실패했습니다.");
    } finally {
      setActionBusy(null);
    }
  };

  useEffect(() => {
    if (!userId) return;
    let cancelled = false;
    setWorkspaces(null);
    membersApi
      .workspaces(userId, undefined, page)
      .then((res) => {
        if (cancelled) return;
        setWorkspaces(res.content);
        setPageInfo({ totalPages: res.page.totalPages, totalElements: res.page.totalElements });
      })
      .catch((e) => {
        if (!cancelled) setError(e instanceof ApiError ? e.message : String(e));
      });
    return () => {
      cancelled = true;
    };
  }, [userId, page]);

  useEffect(() => {
    if (!userId) return;
    let cancelled = false;
    setPayments(null);
    paymentsApi
      .listForUser(userId, paymentPage)
      .then((res) => {
        if (cancelled) return;
        setPayments(res.content);
        setPaymentPageInfo({ totalPages: res.page.totalPages, totalElements: res.page.totalElements });
      })
      .catch((e) => {
        if (!cancelled) setError(e instanceof ApiError ? e.message : String(e));
      });
    return () => {
      cancelled = true;
    };
  }, [userId, paymentPage]);

  return (
    <div>
      <Link to="/users" style={{ fontSize: 13, fontWeight: 600, marginBottom: 10, display: "inline-block" }}>
        ← 사용자 목록으로
      </Link>
      <PageHeader title={member?.email ?? "사용자 상세"} />

      <ErrorAlert message={error} />

      {member && (
        <div className="card">
          <div className="form-row">
            <div>
              <div className="stat-label">가입 경로</div>
              <div style={{ marginTop: 4 }}>{member.provider}</div>
            </div>
            <div>
              <div className="stat-label">권한</div>
              <div style={{ marginTop: 4 }}>
                <span className={`badge ${member.role === "ADMIN" ? "badge-primary" : ""}`}>{member.role}</span>
              </div>
            </div>
            <div>
              <div className="stat-label">요금제</div>
              <div style={{ marginTop: 4 }}>
                <span className={`badge ${member.plan === "PRO" ? "badge-success" : ""}`}>{member.plan}</span>
              </div>
            </div>
            <div>
              <div className="stat-label">Pro 보장 만료(쿠폰/지급)</div>
              <div style={{ marginTop: 4 }}>{formatDate(member.proExpiresAt)}</div>
            </div>
            <div>
              <div className="stat-label">제재 상태</div>
              <div style={{ marginTop: 4 }}>
                {member.banned ? (
                  <span className="badge badge-danger">
                    {member.permanentBan ? "영구 정지" : `일시 정지 (~${formatDate(member.tempBanUntil)})`}
                  </span>
                ) : (
                  <span className="badge">정상</span>
                )}
              </div>
            </div>
            <div>
              <div className="stat-label">가입일</div>
              <div style={{ marginTop: 4 }}>{formatDate(member.createdAt)}</div>
            </div>
          </div>
        </div>
      )}

      {member && (
        <div className="card">
          <h3 style={{ marginTop: 0 }}>회원 관리</h3>

          <div className="form-row" style={{ alignItems: "flex-end" }}>
            <div className="form-field" style={{ maxWidth: 140 }}>
              <label htmlFor="grantDays">Pro 지급/연장 일수</label>
              <input
                id="grantDays"
                type="number"
                min={1}
                value={grantDays}
                onChange={(e) => setGrantDays(e.target.value)}
              />
            </div>
            <button type="button" className="btn btn-primary" onClick={grantPro} disabled={actionBusy === "grant"}>
              {actionBusy === "grant" ? "처리 중..." : "Pro 지급/연장"}
            </button>
            {member.plan === "PRO" && (
              <button type="button" className="btn btn-danger" onClick={downgrade} disabled={actionBusy === "downgrade"}>
                {actionBusy === "downgrade" ? "처리 중..." : "Free로 전환"}
              </button>
            )}
          </div>

          <div className="form-row" style={{ alignItems: "flex-end", marginTop: 4 }}>
            {member.banned ? (
              <button type="button" className="btn btn-primary" onClick={unban} disabled={actionBusy === "unban"}>
                {actionBusy === "unban" ? "처리 중..." : "정지 해제"}
              </button>
            ) : (
              <>
                <div className="form-field">
                  <label htmlFor="banUntil">일시 정지 해제 시각</label>
                  <input
                    id="banUntil"
                    type="datetime-local"
                    value={banUntil}
                    onChange={(e) => setBanUntil(e.target.value)}
                  />
                </div>
                <button type="button" className="btn btn-danger" onClick={() => ban(false)} disabled={actionBusy === "ban"}>
                  {actionBusy === "ban" ? "처리 중..." : "일시 정지"}
                </button>
                <button type="button" className="btn btn-danger" onClick={() => ban(true)} disabled={actionBusy === "ban"}>
                  영구 정지
                </button>
              </>
            )}
          </div>

          {actionMessage && (
            <div className="alert alert-muted" role="status">
              {actionMessage}
            </div>
          )}
        </div>
      )}

      <h3 className="section-heading">워크스페이스</h3>
      {workspaces === null ? (
        <Loading />
      ) : workspaces.length === 0 ? (
        <EmptyState label="워크스페이스가 없습니다." />
      ) : (
        <>
          <div className="table-scroll">
            <table>
              <thead>
                <tr>
                  <th>이름</th>
                  <th>상태</th>
                  <th>생성일</th>
                  <th>수정일</th>
                </tr>
              </thead>
              <tbody>
                {workspaces.map((ws) => (
                  <tr key={ws.id}>
                    <td>{ws.name}</td>
                    <td>
                      <span className={`badge ${ws.status === "ACTIVE" ? "badge-success" : ""}`}>{ws.status}</span>
                    </td>
                    <td>{formatDate(ws.createdAt)}</td>
                    <td>{formatDate(ws.updatedAt)}</td>
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

      <h3 className="section-heading">결제 내역</h3>
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
                  <th>금액</th>
                  <th>상태</th>
                  <th>결제일</th>
                  <th>기록일</th>
                </tr>
              </thead>
              <tbody>
                {payments.map((p) => (
                  <tr key={p.id}>
                    <td>{p.amount.toLocaleString("ko-KR")}원</td>
                    <td>
                      <span className={`badge ${PAYMENT_STATUS_BADGE[p.status]}`}>
                        {PAYMENT_STATUS_LABEL[p.status]}
                      </span>
                    </td>
                    <td>{formatDate(p.paidAt)}</td>
                    <td>{formatDate(p.createdAt)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <Pagination
            page={paymentPage}
            totalPages={paymentPageInfo.totalPages}
            totalElements={paymentPageInfo.totalElements}
            onChange={setPaymentPage}
          />
        </>
      )}
    </div>
  );
}
