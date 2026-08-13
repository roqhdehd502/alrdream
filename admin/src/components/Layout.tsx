import { useState } from "react";
import { NavLink, Outlet } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";
import { getTheme, toggleTheme } from "../theme";
import {
  CouponIcon,
  DashboardIcon,
  MoonIcon,
  PaymentIcon,
  PromptIcon,
  QuotaIcon,
  SubscriptionIcon,
  SunIcon,
  SurveyIcon,
  UsersIcon,
} from "./icons";

const NAV_GROUPS = [
  {
    label: "현황",
    items: [{ to: "/dashboard", label: "대시보드", icon: DashboardIcon }],
  },
  {
    label: "구독",
    items: [
      { to: "/subscriptions", label: "구독 관리", icon: SubscriptionIcon },
      { to: "/payments", label: "결제 관리", icon: PaymentIcon },
      { to: "/prompt-quota", label: "AI 프롬프트 횟수", icon: QuotaIcon },
    ],
  },
  {
    label: "쿠폰",
    items: [
      { to: "/coupons", label: "쿠폰 코드 관리", icon: CouponIcon },
      { to: "/coupons/redemptions", label: "유저 쿠폰 사용 현황", icon: CouponIcon },
    ],
  },
  {
    label: "콘텐츠",
    items: [
      { to: "/surveys", label: "설문 정의", icon: SurveyIcon },
      { to: "/prompt-templates", label: "AI 프롬프트 템플릿", icon: PromptIcon },
    ],
  },
  {
    label: "사용자",
    items: [{ to: "/users", label: "사용자", icon: UsersIcon }],
  },
];

export function Layout() {
  const { member, logout } = useAuth();
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const [theme, setThemeState] = useState(getTheme);

  const closeSidebar = () => setSidebarOpen(false);
  const handleToggleTheme = () => setThemeState(toggleTheme());

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

        {NAV_GROUPS.map((group) => (
          <div key={group.label}>
            <div className="sidebar-nav-label">{group.label}</div>
            {group.items.map((item) => (
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
          </div>
        ))}

        <div className="sidebar-footer">
          <div className="sidebar-user">{member?.email}</div>
          <button
            type="button"
            className="btn btn-block"
            style={{ marginBottom: 8 }}
            onClick={handleToggleTheme}
          >
            {theme === "dark" ? <SunIcon size={15} /> : <MoonIcon size={15} />}
            {theme === "dark" ? "라이트 모드" : "다크 모드"}
          </button>
          <button type="button" className="btn btn-block" onClick={logout}>
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
