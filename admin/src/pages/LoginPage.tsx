import { useState, type FormEvent } from "react";
import { Navigate } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";
import { authApi } from "../api/auth";
import { ApiError } from "../api/client";
import { getTheme, toggleTheme } from "../theme";
import { MoonIcon, SunIcon } from "../components/icons";

type Mode = "login" | "reset-request" | "reset-confirm";

export function LoginPage() {
  const { status, login } = useAuth();
  const [mode, setMode] = useState<Mode>("login");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [code, setCode] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [info, setInfo] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [theme, setThemeState] = useState(getTheme);

  if (status === "authenticated") {
    return <Navigate to="/dashboard" replace />;
  }

  const resetMessages = () => {
    setError(null);
    setInfo(null);
  };

  const handleLogin = async (e: FormEvent) => {
    e.preventDefault();
    resetMessages();
    setSubmitting(true);
    try {
      await login(email, password);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "로그인에 실패했습니다.");
    } finally {
      setSubmitting(false);
    }
  };

  const handleRequestReset = async (e: FormEvent) => {
    e.preventDefault();
    resetMessages();
    setSubmitting(true);
    try {
      await authApi.requestPasswordReset(email);
      setInfo("이메일로 재설정 코드를 보냈습니다(가입된 이메일인 경우). 코드를 입력해주세요.");
      setMode("reset-confirm");
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "요청에 실패했습니다.");
    } finally {
      setSubmitting(false);
    }
  };

  const handleConfirmReset = async (e: FormEvent) => {
    e.preventDefault();
    resetMessages();
    setSubmitting(true);
    try {
      await authApi.confirmPasswordReset(email, code, newPassword);
      setMode("login");
      setPassword("");
      setCode("");
      setNewPassword("");
      setInfo("비밀번호가 재설정되었습니다. 새 비밀번호로 로그인해주세요.");
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "재설정에 실패했습니다.");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="login-shell">
      <button
        type="button"
        className="theme-toggle-fab"
        aria-label="테마 전환"
        onClick={() => setThemeState(toggleTheme())}
      >
        {theme === "dark" ? <SunIcon size={16} /> : <MoonIcon size={16} />}
      </button>
      <div className="login-card">
        <img src="/favicon.svg" alt="" className="login-logo" />
        <h1>알려드림 Admin</h1>

        {mode === "login" && (
          <>
            <p>관리자 계정으로 로그인하세요.</p>
            <form onSubmit={handleLogin}>
              <div className="form-field">
                <label htmlFor="email">이메일</label>
                <input
                  id="email"
                  type="email"
                  autoComplete="username"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  required
                  autoFocus
                />
              </div>
              <div className="form-field">
                <label htmlFor="password">비밀번호</label>
                <input
                  id="password"
                  type="password"
                  autoComplete="current-password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  required
                />
              </div>
              {error && (
                <div className="alert alert-error" role="alert">
                  {error}
                </div>
              )}
              {info && <div className="alert alert-success">{info}</div>}
              <button type="submit" className="btn btn-primary btn-block" disabled={submitting}>
                {submitting ? "로그인 중..." : "로그인"}
              </button>
            </form>
            <button
              type="button"
              className="btn-link"
              onClick={() => {
                resetMessages();
                setMode("reset-request");
              }}
            >
              비밀번호를 잊으셨나요?
            </button>
          </>
        )}

        {mode === "reset-request" && (
          <>
            <p>가입한 이메일로 6자리 재설정 코드를 보내드려요.</p>
            <form onSubmit={handleRequestReset}>
              <div className="form-field">
                <label htmlFor="reset-email">이메일</label>
                <input
                  id="reset-email"
                  type="email"
                  autoComplete="username"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  required
                  autoFocus
                />
              </div>
              {error && (
                <div className="alert alert-error" role="alert">
                  {error}
                </div>
              )}
              <button type="submit" className="btn btn-primary btn-block" disabled={submitting}>
                {submitting ? "요청 중..." : "재설정 코드 받기"}
              </button>
            </form>
            <button type="button" className="btn-link" onClick={() => { resetMessages(); setMode("login"); }}>
              로그인으로 돌아가기
            </button>
          </>
        )}

        {mode === "reset-confirm" && (
          <>
            <p>{info ?? "이메일로 받은 코드와 새 비밀번호를 입력해주세요."}</p>
            <form onSubmit={handleConfirmReset}>
              <div className="form-field">
                <label htmlFor="reset-code">6자리 코드</label>
                <input
                  id="reset-code"
                  type="text"
                  inputMode="numeric"
                  maxLength={6}
                  value={code}
                  onChange={(e) => setCode(e.target.value)}
                  required
                  autoFocus
                />
              </div>
              <div className="form-field">
                <label htmlFor="new-password">새 비밀번호 (8자 이상)</label>
                <input
                  id="new-password"
                  type="password"
                  autoComplete="new-password"
                  value={newPassword}
                  onChange={(e) => setNewPassword(e.target.value)}
                  required
                  minLength={8}
                />
              </div>
              {error && (
                <div className="alert alert-error" role="alert">
                  {error}
                </div>
              )}
              <button type="submit" className="btn btn-primary btn-block" disabled={submitting}>
                {submitting ? "재설정 중..." : "비밀번호 재설정"}
              </button>
            </form>
            <button type="button" className="btn-link" onClick={() => { resetMessages(); setMode("login"); }}>
              로그인으로 돌아가기
            </button>
          </>
        )}
      </div>
    </div>
  );
}
