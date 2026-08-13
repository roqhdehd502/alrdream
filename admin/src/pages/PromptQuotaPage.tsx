import { useEffect, useState } from "react";
import { settingsApi } from "../api/settings";
import { ApiError } from "../api/client";
import { PageHeader } from "../components/PageHeader";
import { ErrorAlert } from "../components/Feedback";

export function PromptQuotaPage() {
  const [current, setCurrent] = useState<{ freeMonthlyLimit: number; proMonthlyLimit: number } | null>(null);
  const [freeInput, setFreeInput] = useState("");
  const [proInput, setProInput] = useState("");
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    settingsApi
      .getFreeTierLimit()
      .then((res) => {
        setCurrent({ freeMonthlyLimit: res.freeMonthlyLimit, proMonthlyLimit: res.proMonthlyLimit });
        setFreeInput(String(res.freeMonthlyLimit));
        setProInput(String(res.proMonthlyLimit));
      })
      .catch((e) => setError(e instanceof ApiError ? e.message : String(e)));
  }, []);

  const parseLimit = (raw: string) => {
    if (raw.trim() === "") return null;
    const value = Number(raw);
    return Number.isInteger(value) && value >= 0 ? value : null;
  };

  const saveLimits = async () => {
    if (current === null) {
      setMessage("현재 값을 불러오지 못해 저장할 수 없습니다. 새로고침 후 다시 시도해주세요.");
      return;
    }
    const freeMonthlyLimit = parseLimit(freeInput);
    const proMonthlyLimit = parseLimit(proInput);
    if (freeMonthlyLimit === null || proMonthlyLimit === null) {
      setMessage("0 이상의 정수를 입력해주세요.");
      return;
    }
    setSaving(true);
    setMessage(null);
    try {
      const res = await settingsApi.updateFreeTierLimit(freeMonthlyLimit, proMonthlyLimit);
      setCurrent({ freeMonthlyLimit: res.freeMonthlyLimit, proMonthlyLimit: res.proMonthlyLimit });
      setMessage("저장되었습니다.");
    } catch (e) {
      setMessage(e instanceof ApiError ? e.message : "저장에 실패했습니다.");
    } finally {
      setSaving(false);
    }
  };

  return (
    <div>
      <PageHeader title="AI 프롬프트 횟수" description="FREE/PRO 플랜의 월별 AI 생성 횟수 한도를 관리합니다." />

      <ErrorAlert message={error} />

      <div className="card">
        <h3 style={{ marginTop: 0, marginBottom: 4 }}>플랜별 월별 생성 횟수 한도</h3>
        <p style={{ color: "var(--color-text-muted)", fontSize: 13, marginTop: 0, marginBottom: 16 }}>
          현재 값: <strong>FREE {current?.freeMonthlyLimit ?? "-"}회</strong> /{" "}
          <strong>PRO {current?.proMonthlyLimit ?? "-"}회</strong> (월). 변경 시 이후 새로 시작되는 달부터
          적용됩니다.
        </p>
        <div className="form-row" style={{ alignItems: "flex-end" }}>
          <div className="form-field" style={{ maxWidth: 160 }}>
            <label htmlFor="free-limit">FREE 월별 한도(회)</label>
            <input
              id="free-limit"
              type="number"
              min={0}
              value={freeInput}
              onChange={(e) => setFreeInput(e.target.value)}
            />
          </div>
          <div className="form-field" style={{ maxWidth: 160 }}>
            <label htmlFor="pro-limit">PRO 월별 한도(회)</label>
            <input
              id="pro-limit"
              type="number"
              min={0}
              value={proInput}
              onChange={(e) => setProInput(e.target.value)}
            />
          </div>
          <button type="button" className="btn btn-primary" onClick={saveLimits} disabled={saving || current === null}>
            {saving ? "저장 중..." : "저장"}
          </button>
        </div>
        {message && (
          <div className="alert alert-muted" role="status">
            {message}
          </div>
        )}
      </div>
    </div>
  );
}
