import { useState } from "react";
import { NavLink, Outlet } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";
import { DashboardIcon, PromptIcon, SurveyIcon, UsersIcon } from "./icons";

const NAV_ITEMS = [
  { to: "/dashboard", label: "대시보드", icon: DashboardIcon },
  { to: "/surveys", label: "설문 정의", icon: SurveyIcon },
  { to: "/prompt-templates", label: "AI 프롬프트 템플릿", icon: PromptIcon },
  { to: "/users", label: "사용자", icon: UsersIcon },
];

export function Layout() {
  const { member, logout } = useAuth();
  const [sidebarOpen, setSidebarOpen] = useState(false);

  const closeSidebar = () => setSidebarOpen(false);

  return (
    <div className="app-shell">
      {sidebarOpen && <div className="sidebar-backdrop" onClick={closeSidebar} />}

      <aside className={`sidebar${sidebarOpen ? " open" : ""}`}>
        <div className="sidebar-brand">
          <img src="/favicon.svg" alt="" />
          <div className="sidebar-brand-text">
            알려드림
            <small>Admin Console</small>
          </div>
        </div>

        <div className="sidebar-nav-label">메뉴</div>
        {NAV_ITEMS.map((item) => (
          <NavLink
            key={item.to}
            to={item.to}
            className={({ isActive }) => `sidebar-link${isActive ? " active" : ""}`}
            onClick={closeSidebar}
          >
            <item.icon />
            {item.label}
          </NavLink>
        ))}

        <div className="sidebar-footer">
          <div className="sidebar-user">{member?.email}</div>
          <button type="button" className="btn" style={{ width: "100%" }} onClick={logout}>
            로그아웃
          </button>
        </div>
      </aside>

      <div className="main-area">
        <div className="topbar">
          <button
            type="button"
            className="topbar-menu-button"
            onClick={() => setSidebarOpen((open) => !open)}
            aria-label="메뉴 열기"
          >
            ☰
          </button>
          <img src="/favicon.svg" alt="" style={{ width: 20, height: 19 }} />
          <strong>알려드림 Admin</strong>
        </div>
        <div className="page-content">
          <Outlet />
        </div>
      </div>
    </div>
  );
}
