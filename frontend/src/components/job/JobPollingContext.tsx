import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState, type ReactNode } from "react";
import { aiJobsApi } from "../../api/aiJobs";
import { ApiError } from "../../api/client";
import { useAuth } from "../../auth/AuthContext";
import type { JobStatus } from "../../types";

const POLL_INTERVAL_MS = 2000;

export interface TrackedJob {
  jobId: string;
  redirectTo?: string;
  status: JobStatus;
  errorMessage: string | null;
}

interface JobPollingContextValue {
  jobs: TrackedJob[];
  getJob: (jobId: string) => TrackedJob | undefined;
  startTracking: (jobId: string, redirectTo?: string) => void;
  dismiss: (jobId: string) => void;
}

const JobPollingContext = createContext<JobPollingContextValue | null>(null);

/**
 * Phase 16 — 이전엔 `generating.tsx` 화면이 폴링 타이머를 직접 소유해, 화면을 벗어나면(뒤로가기 등) 완료
 * 여부를 알 방법이 없었다(화면 안내 문구와 달리 실제로는 폴링이 끊겼음). 폴링을 화면 생명주기와 분리된
 * 이 Provider(앱 루트에 마운트)로 옮겨, 어느 화면에 있든 계속 진행 상태를 추적할 수 있게 한다.
 *
 * <p>Phase 21 전수 점검 — 서로 다른 워크스페이스에서 거의 동시에 재생성을 시작할 수 있는데(예: A 생성 중에
 * B도 재생성 시작), 전역에 job을 하나만 추적하던 예전 구조는 나중 job이 먼저 것을 조용히 덮어써 첫 job의
 * 완료/실패를 영영 알 수 없게 만들었다. jobId를 key로 하는 Map으로 바꿔 여러 job을 동시에 추적한다.
 */
export function JobPollingProvider({ children }: { children: ReactNode }) {
  const { status } = useAuth();
  const [jobs, setJobs] = useState<Map<string, TrackedJob>>(new Map());
  const timersRef = useRef<Map<string, ReturnType<typeof setTimeout>>>(new Map());
  const activeRef = useRef<Set<string>>(new Set()); // 폴링이 계속 진행 중인 jobId만 담음(완료/실패는 제거).

  const stopPolling = useCallback((jobId: string) => {
    const timer = timersRef.current.get(jobId);
    if (timer) {
      clearTimeout(timer);
      timersRef.current.delete(jobId);
    }
    activeRef.current.delete(jobId);
  }, []);

  const stopAllPolling = useCallback(() => {
    timersRef.current.forEach((timer) => clearTimeout(timer));
    timersRef.current.clear();
    activeRef.current.clear();
  }, []);

  const putJob = useCallback((jobId: string, next: TrackedJob) => {
    setJobs((prev) => {
      const map = new Map(prev);
      map.set(jobId, next);
      return map;
    });
  }, []);

  const startTracking = useCallback(
    (jobId: string, redirectTo?: string) => {
      if (activeRef.current.has(jobId)) return; // 이미 추적 중인 job이면 중복 폴링을 시작하지 않는다.
      activeRef.current.add(jobId);
      putJob(jobId, { jobId, redirectTo, status: "PENDING", errorMessage: null });

      const poll = async () => {
        if (!activeRef.current.has(jobId)) return; // 그 사이 dismiss됨 — 이 응답은 버린다.
        try {
          const res = await aiJobsApi.get(jobId);
          if (!activeRef.current.has(jobId)) return;
          if (res.status === "COMPLETED" || res.status === "FAILED") {
            activeRef.current.delete(jobId); // 폴링은 멈추되, 완료/실패 상태는 사용자가 확인할 때까지 유지한다.
            putJob(jobId, { jobId, redirectTo, status: res.status, errorMessage: res.errorMessage ?? null });
            return;
          }
          putJob(jobId, { jobId, redirectTo, status: res.status, errorMessage: null });
          timersRef.current.set(jobId, setTimeout(poll, POLL_INTERVAL_MS));
        } catch (e) {
          if (!activeRef.current.has(jobId)) return;
          activeRef.current.delete(jobId);
          putJob(jobId, {
            jobId,
            redirectTo,
            status: "FAILED",
            errorMessage: e instanceof ApiError ? e.message : "상태 조회에 실패했습니다.",
          });
        }
      };
      poll();
    },
    [putJob],
  );

  const dismiss = useCallback(
    (jobId: string) => {
      stopPolling(jobId);
      setJobs((prev) => {
        if (!prev.has(jobId)) return prev;
        const map = new Map(prev);
        map.delete(jobId);
        return map;
      });
    },
    [stopPolling],
  );

  const dismissAll = useCallback(() => {
    stopAllPolling();
    setJobs(new Map());
  }, [stopAllPolling]);

  // 로그아웃/탈퇴/세션 만료로 인증 상태가 풀리면 이전 계정이 추적하던 job들을 모두 정리한다 — 그렇지
  // 않으면 같은 기기에서 곧바로 다른 계정으로 로그인했을 때 이전 계정의 배너가 새 세션에 노출되고, 탭하면
  // 이전 계정 소유의 워크스페이스 경로로 이동할 수 있었다(Phase 21 전수 점검에서 발견). account.tsx의
  // syncedName처럼 "렌더 중 조정" 패턴을 쓸 수는 없다 — dismissAll이 timersRef/activeRef(ref)도 함께
  // 정리하는데, ref는 렌더 중에 접근하면 안 되고(react-hooks/refs) 반드시 effect/이벤트 핸들러 안에서만
  // 건드려야 한다. 타이머 해제라는 외부 시스템 정리가 필요한 진짜 effect라 set-state-in-effect는 의도적으로 끈다.
  useEffect(() => {
    if (status === "unauthenticated") {
      // eslint-disable-next-line react-hooks/set-state-in-effect
      dismissAll();
    }
  }, [status, dismissAll]);

  useEffect(() => stopAllPolling, [stopAllPolling]);

  const jobsList = useMemo(() => Array.from(jobs.values()), [jobs]);
  const getJob = useCallback((jobId: string) => jobs.get(jobId), [jobs]);

  const value = useMemo(
    () => ({ jobs: jobsList, getJob, startTracking, dismiss }),
    [jobsList, getJob, startTracking, dismiss],
  );
  return <JobPollingContext.Provider value={value}>{children}</JobPollingContext.Provider>;
}

export function useJobPolling(): JobPollingContextValue {
  const ctx = useContext(JobPollingContext);
  if (!ctx) throw new Error("useJobPolling은 JobPollingProvider 내부에서만 쓸 수 있습니다.");
  return ctx;
}
