import { useEffect, useState } from "react";
import { settingsApi } from "../api/settings";
import { ApiError } from "../api/client";
import { PageHeader } from "../components/PageHeader";
import { ErrorAlert } from "../components/Feedback";

export function PromptQuotaPage() {
  const [freeTierLimit, setFreeTierLimit] = useState<number | null>(null);
  const [limitInput, setLimitInput] = useState("");
  const [limitSaving, setLimitSaving] = useState(false);
  const [limitMessage, setLimitMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    settingsApi
      .getFreeTierLimit()
      .then((res) => {
        setFreeTierLimit(res.monthlyLimit);
        setLimitInput(String(res.monthlyLimit));
      })
      .catch((e) => setError(e instanceof ApiError ? e.message : String(e)));
  }, []);

  const saveLimit = async () => {
    if (freeTierLimit === null) {
      setLimitMessage("현재 값을 불러오지 못해 저장할 수 없습니다. 새로고침 후 다시 시도해주세요.");
      return;
    }
    if (limitInput.trim() === "") {
      setLimitMessage("0 이상의 정수를 입력해주세요.");
      return;
    }
    const value = Number(limitInput);
    if (!Number.isInteger(value) || value < 0) {
      setLimitMessage("0 이상의 정수를 입력해주세요.");
      return;
    }
    setLimitSaving(true);
    setLimitMessage(null);
    try {
      const res = await settingsApi.updateFreeTierLimit(value);
      setFreeTierLimit(res.monthlyLimit);
      setLimitMessage("저장되었습니다.");
    } catch (e) {
      setLimitMessage(e instanceof ApiError ? e.message : "저장에 실패했습니다.");
    } finally {
      setLimitSaving(false);
    }
  };

  return (
    <div>
      <PageHeader title="AI 프롬프트 횟수" description="FREE 플랜의 월별 AI 생성 횟수 한도를 관리합니다." />

      <ErrorAlert message={error} />

      <div className="card">
        <h3 style={{ marginTop: 0, marginBottom: 4 }}>FREE 플랜 월별 생성 횟수 한도</h3>
        <p style={{ color: "var(--color-text-muted)", fontSize: 13, marginTop: 0, marginBottom: 16 }}>
          현재 값: <strong>{freeTierLimit ?? "-"}회 / 월</strong>. 변경 시 이후 새로 시작되는 달부터 적용됩니다.
        </p>
        <div className="form-row" style={{ alignItems: "flex-end" }}>
          <div className="form-field" style={{ maxWidth: 160 }}>
            <label htmlFor="limit">월별 한도(회)</label>
            <input
              id="limit"
              type="number"
              min={0}
              value={limitInput}
              onChange={(e) => setLimitInput(e.target.value)}
            />
          </div>
          <button
            type="button"
            className="btn btn-primary"
            onClick={saveLimit}
            disabled={limitSaving || freeTierLimit === null}
          >
            {limitSaving ? "저장 중..." : "저장"}
          </button>
        </div>
        {limitMessage && (
          <div className="alert alert-muted" role="status">
            {limitMessage}
          </div>
        )}
      </div>
    </div>
  );
}
