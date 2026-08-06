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

  useEffect(() => {
    setMembers(null);
    membersApi
      .list(keyword, page)
      .then((res) => {
        setMembers(res.content);
        setPageInfo({ totalPages: res.page.totalPages, totalElements: res.page.totalElements });
      })
      .catch((e) => setError(e instanceof ApiError ? e.message : String(e)));
  }, [keyword, page]);

  return (
    <div>
      <PageHeader title="사용자 조회" description="CS 대응용 조회 화면입니다. 이 화면에서는 수정/삭제를 지원하지 않습니다." />

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
                  <th>이메일</th>
                  <th>가입 경로</th>
                  <th>권한</th>
                  <th>요금제</th>
                  <th>가입일</th>
                </tr>
              </thead>
              <tbody>
                {members.map((member) => (
                  <tr key={member.id} className="clickable" onClick={() => navigate(`/users/${member.id}`)}>
                    <td>{member.email}</td>
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
