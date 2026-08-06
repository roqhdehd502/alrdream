import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { membersApi } from "../api/members";
import { ApiError } from "../api/client";
import { Pagination } from "../components/Pagination";
import { EmptyState, ErrorAlert, Loading } from "../components/Feedback";
import type { MemberAdminResponse, WorkspaceResponse } from "../types";

function formatDate(value: string) {
  return new Date(value).toLocaleString("ko-KR");
}

export function UserDetailPage() {
  const { userId } = useParams<{ userId: string }>();
  const [member, setMember] = useState<MemberAdminResponse | null>(null);
  const [workspaces, setWorkspaces] = useState<WorkspaceResponse[] | null>(null);
  const [page, setPage] = useState(0);
  const [pageInfo, setPageInfo] = useState({ totalPages: 0, totalElements: 0 });
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!userId) return;
    membersApi.get(userId).then(setMember).catch((e) => setError(e instanceof ApiError ? e.message : String(e)));
  }, [userId]);

  useEffect(() => {
    if (!userId) return;
    setWorkspaces(null);
    membersApi
      .workspaces(userId, undefined, page)
      .then((res) => {
        setWorkspaces(res.content);
        setPageInfo({ totalPages: res.page.totalPages, totalElements: res.page.totalElements });
      })
      .catch((e) => setError(e instanceof ApiError ? e.message : String(e)));
  }, [userId, page]);

  return (
    <div>
      <div className="page-header">
        <div>
          <Link to="/users" style={{ fontSize: 13, fontWeight: 600 }}>
            ← 사용자 목록으로
          </Link>
          <h1 style={{ marginTop: 10 }}>{member?.email ?? "사용자 상세"}</h1>
        </div>
      </div>

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
              <div className="stat-label">가입일</div>
              <div style={{ marginTop: 4 }}>{formatDate(member.createdAt)}</div>
            </div>
          </div>
        </div>
      )}

      <h3 style={{ fontSize: 15 }}>워크스페이스</h3>
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
                    <td>{ws.status}</td>
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
    </div>
  );
}
