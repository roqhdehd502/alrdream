import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState, type ReactNode } from "react";
import { aiJobsApi } from "../../api/aiJobs";
import { ApiError } from "../../api/client";
import type { JobStatus } from "../../types";

const POLL_INTERVAL_MS = 2000;

export interface TrackedJob {
  jobId: string;
  redirectTo?: string;
  status: JobStatus;
  errorMessage: string | null;
}

interface JobPollingContextValue {
  job: TrackedJob | null;
  startTracking: (jobId: string, redirectTo?: string) => void;
  dismiss: () => void;
}

const JobPollingContext = createContext<JobPollingContextValue | null>(null);

/**
 * Phase 16 — 이전엔 `generating.tsx` 화면이 폴링 타이머를 직접 소유해, 화면을 벗어나면(뒤로가기 등) 완료
 * 여부를 알 방법이 없었다(화면 안내 문구와 달리 실제로는 폴링이 끊겼음). 폴링을 화면 생명주기와 분리된
 * 이 Provider(앱 루트에 마운트)로 옮겨, 어느 화면에 있든 계속 진행 상태를 추적할 수 있게 한다.
 */
export function JobPollingProvider({ children }: { children: ReactNode }) {
  const [job, setJob] = useState<TrackedJob | null>(null);
  const timerRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const trackedJobIdRef = useRef<string | null>(null);

  const stopPolling = useCallback(() => {
    if (timerRef.current) {
      clearTimeout(timerRef.current);
      timerRef.current = null;
    }
    trackedJobIdRef.current = null;
  }, []);

  const startTracking = useCallback(
    (jobId: string, redirectTo?: string) => {
      if (trackedJobIdRef.current === jobId) return; // 이미 추적 중인 job이면 중복 폴링을 시작하지 않는다.
      stopPolling();
      trackedJobIdRef.current = jobId;
      setJob({ jobId, redirectTo, status: "PENDING", errorMessage: null });

      const poll = async () => {
        if (trackedJobIdRef.current !== jobId) return; // 그 사이 다른 job 추적으로 교체됨 — 이 응답은 버린다.
        try {
          const res = await aiJobsApi.get(jobId);
          if (trackedJobIdRef.current !== jobId) return;
          if (res.status === "COMPLETED" || res.status === "FAILED") {
            trackedJobIdRef.current = null; // 폴링은 멈추되, 완료/실패 상태는 사용자가 확인할 때까지 유지한다.
            setJob({ jobId, redirectTo, status: res.status, errorMessage: res.errorMessage ?? null });
            return;
          }
          setJob({ jobId, redirectTo, status: res.status, errorMessage: null });
          timerRef.current = setTimeout(poll, POLL_INTERVAL_MS);
        } catch (e) {
          if (trackedJobIdRef.current !== jobId) return;
          trackedJobIdRef.current = null;
          setJob({
            jobId,
            redirectTo,
            status: "FAILED",
            errorMessage: e instanceof ApiError ? e.message : "상태 조회에 실패했습니다.",
          });
        }
      };
      poll();
    },
    [stopPolling],
  );

  const dismiss = useCallback(() => {
    stopPolling();
    setJob(null);
  }, [stopPolling]);

  useEffect(() => stopPolling, [stopPolling]);

  const value = useMemo(() => ({ job, startTracking, dismiss }), [job, startTracking, dismiss]);
  return <JobPollingContext.Provider value={value}>{children}</JobPollingContext.Provider>;
}

export function useJobPolling(): JobPollingContextValue {
  const ctx = useContext(JobPollingContext);
  if (!ctx) throw new Error("useJobPolling은 JobPollingProvider 내부에서만 쓸 수 있습니다.");
  return ctx;
}
