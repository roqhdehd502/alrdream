import { useEffect, useRef, useState } from "react";
import { ActivityIndicator, Text, View } from "react-native";
import { useLocalSearchParams, useRouter, type Href } from "expo-router";
import { aiJobsApi } from "../../api/aiJobs";
import { ApiError } from "../../api/client";
import { Button } from "../../components/ui/Button";
import { useTheme, useThemedStyles } from "../../components/ui/ThemeContext";
import type { JobStatus } from "../../types";

const POLL_INTERVAL_MS = 2000;

const STATUS_LABEL: Record<JobStatus, string> = {
  PENDING: "생성 대기 중",
  PROCESSING: "AI가 생성하고 있어요",
  COMPLETED: "완료됐습니다",
  FAILED: "생성에 실패했습니다",
};

export default function GeneratingScreen() {
  const router = useRouter();
  const { jobId, redirectTo } = useLocalSearchParams<{ jobId: string; workspaceId?: string; redirectTo?: string }>();
  const { colors, typography } = useTheme();
  const styles = useThemedStyles((colors) => ({
    root: { flex: 1, alignItems: "center" as const, justifyContent: "center" as const, gap: 12, padding: 24, backgroundColor: colors.bg },
    status: { marginTop: 8 },
    message: { textAlign: "center" as const },
    backButton: { marginTop: 16 },
  }));
  const [status, setStatus] = useState<JobStatus>("PENDING");
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const timerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  useEffect(() => {
    let cancelled = false;

    const poll = async () => {
      try {
        const job = await aiJobsApi.get(jobId);
        if (cancelled) return;
        setStatus(job.status);
        if (job.status === "COMPLETED") {
          router.replace((redirectTo ?? "/") as Href);
          return;
        }
        if (job.status === "FAILED") {
          setErrorMessage(job.errorMessage ?? "알 수 없는 오류가 발생했습니다.");
          return;
        }
        timerRef.current = setTimeout(poll, POLL_INTERVAL_MS);
      } catch (e) {
        if (cancelled) return;
        setErrorMessage(e instanceof ApiError ? e.message : "상태 조회에 실패했습니다.");
      }
    };

    poll();
    return () => {
      cancelled = true;
      if (timerRef.current) clearTimeout(timerRef.current);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [jobId]);

  return (
    <View style={styles.root}>
      {status === "FAILED" || errorMessage ? (
        <>
          <Text style={typography.heading}>생성에 실패했습니다</Text>
          <Text style={[typography.muted, styles.message]}>{errorMessage ?? "잠시 후 다시 시도해주세요."}</Text>
          <Button
            label="워크스페이스로 돌아가기"
            onPress={() => router.replace((redirectTo ?? "/") as Href)}
            style={styles.backButton}
          />
        </>
      ) : (
        <>
          <ActivityIndicator size="large" color={colors.primary} />
          <Text style={[typography.heading, styles.status]}>{STATUS_LABEL[status]}</Text>
          <Text style={typography.muted}>보통 수십 초 정도 걸려요. 화면을 벗어나도 계속 진행됩니다.</Text>
        </>
      )}
    </View>
  );
}
