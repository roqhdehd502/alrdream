import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { membersApi } from "../api/members";
import { ApiError } from "../api/client";
import { Pagination } from "../components/Pagination";
import { PageHeader } from "../components/PageHeader";
import { EmptyState, ErrorAlert, Loading } from "../components/Feedback";
import type { MemberAdminResponse } from "../types";

function formatDate(value: string) {
  return new Date(value).toLocaleString("ko-KR");
}

export function UsersPage() {
  const navigate = useNavigate();
  const [keywordInput, setKeywordInput] = useState("");
  const [keyword, setKeyword] = useState<string | undefined>(undefined);
  const [page, setPage] = useState(0);
  const [members, setMembers] = useState<MemberAdminResponse[] | null>(null);
  const [pageInfo, setPageInfo] = useState({ totalPages: 0, totalElements: 0 });
  const [error, setError] = useState<string | null>(null);

  const [selectedIds, setSelectedIds] = useState<Set<string>>(new Set());
  const [grantDays, setGrantDays] = useState("");
  const [granting, setGranting] = useState(false);
  const [grantMessage, setGrantMessage] = useState<string | null>(null);

  const load = () => {
    setMembers(null);
    membersApi
      .list(keyword, page)
      .then((res) => {
        setMembers(res.content);
        setPageInfo({ totalPages: res.page.totalPages, totalElements: res.page.totalElements });
      })
      .catch((e) => setError(e instanceof ApiError ? e.message : String(e)));
  };

  useEffect(() => {
    let cancelled = false;
    setMembers(null);
    membersApi
      .list(keyword, page)
      .then((res) => {
        if (cancelled) return;
        setMembers(res.content);
        setPageInfo({ totalPages: res.page.totalPages, totalElements: res.page.totalElements });
      })
      .catch((e) => {
        if (!cancelled) setError(e instanceof ApiError ? e.message : String(e));
      });
    // 필터/페이지를 빠르게 바꾸면 먼저 보낸 요청이 나중에 도착해 최신 화면을 덮어쓸 수 있어(stale response),
    // 언마운트/재실행 시 이전 요청의 결과 반영을 막는다.
    return () => {
      cancelled = true;
    };
  }, [keyword, page]);

  useEffect(() => {
    setSelectedIds(new Set());
  }, [keyword, page]);

  const toggleSelect = (id: string) => {
    setSelectedIds((prev) => {
      const next = new Set(prev);
      if (next.has(id)) {
        next.delete(id);
      } else {
        next.add(id);
      }
      return next;
    });
  };

  const applyBulkGrant = async () => {
    const days = Number(grantDays);
    if (!Number.isInteger(days) || days < 1) {
      setGrantMessage("일수는 1 이상의 정수를 입력해주세요.");
      return;
    }
    setGranting(true);
    setGrantMessage(null);
    try {
      await membersApi.bulkGrantPro(Array.from(selectedIds), days);
      setGrantMessage(`${selectedIds.size}명에게 Pro ${days}일을 지급/연장했습니다.`);
      setSelectedIds(new Set());
      setGrantDays("");
      load();
    } catch (e) {
      setGrantMessage(e instanceof ApiError ? e.message : "지급에 실패했습니다.");
    } finally {
      setGranting(false);
    }
  };

  return (
    <div>
      <PageHeader title="사용자 조회" description="CS 대응용 조회 화면 — 체크박스로 여러 명을 선택해 Pro를 일괄 지급/연장할 수 있습니다." />

      <ErrorAlert message={error} />

      <div className="toolbar">
        <form
          onSubmit={(e) => {
            e.preventDefault();
            setKeyword(keywordInput.trim() || undefined);
            setPage(0);
          }}
          style={{ display: "flex", gap: 8 }}
        >
          <input
            type="text"
            className="search-input"
            placeholder="이메일로 검색"
            value={keywordInput}
            onChange={(e) => setKeywordInput(e.target.value)}
          />
          <button type="submit" className="btn">
            검색
          </button>
        </form>
      </div>

      {selectedIds.size > 0 && (
        <div className="card card--nested" style={{ marginBottom: 16 }}>
          <div className="form-row" style={{ alignItems: "flex-end" }}>
            <div className="form-field" style={{ maxWidth: 140 }}>
              <label htmlFor="grantDays">{selectedIds.size}명 선택됨 — Pro 지급/연장 일수</label>
              <input
                id="grantDays"
                type="number"
                min={1}
                value={grantDays}
                onChange={(e) => setGrantDays(e.target.value)}
              />
            </div>
            <button type="button" className="btn btn-primary" onClick={applyBulkGrant} disabled={granting}>
              {granting ? "처리 중..." : "일괄 지급"}
            </button>
            <button type="button" className="btn" onClick={() => setSelectedIds(new Set())}>
              선택 해제
            </button>
          </div>
          {grantMessage && (
            <div className="alert alert-muted" role="status">
              {grantMessage}
            </div>
          )}
        </div>
      )}

      {members === null ? (
        <Loading />
      ) : members.length === 0 ? (
        <EmptyState label="검색 결과가 없습니다." />
      ) : (
        <>
          <div className="table-scroll">
            <table>
              <thead>
                <tr>
                  <th style={{ width: 32 }}></th>
                  <th>이메일</th>
                  <th>이름</th>
                  <th>가입 경로</th>
                  <th>권한</th>
                  <th>요금제</th>
                  <th>상태</th>
                  <th>가입일</th>
                </tr>
              </thead>
              <tbody>
                {members.map((member) => (
                  <tr key={member.id} className="clickable" onClick={() => navigate(`/users/${member.id}`)}>
                    <td onClick={(e) => e.stopPropagation()}>
                      <input
                        type="checkbox"
                        checked={selectedIds.has(member.id)}
                        onChange={() => toggleSelect(member.id)}
                      />
                    </td>
                    <td>{member.email}</td>
                    <td>{member.name ?? "-"}</td>
                    <td>{member.provider}</td>
                    <td>
                      {member.role === "ADMIN" ? (
                        <span className="badge badge-primary">ADMIN</span>
                      ) : (
                        <span className="badge">USER</span>
                      )}
                    </td>
                    <td>
                      {member.plan === "PRO" ? (
                        <span className="badge badge-success">PRO</span>
                      ) : (
                        <span className="badge">FREE</span>
                      )}
                    </td>
                    <td>{member.banned && <span className="badge badge-danger">제재중</span>}</td>
                    <td>{formatDate(member.createdAt)}</td>
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
