import { useEffect } from "react";
import { ActivityIndicator, Text, View } from "react-native";
import { useLocalSearchParams, useRouter, type Href } from "expo-router";
import { Button } from "../../components/ui/Button";
import { ScreenContainer } from "../../components/ui/ScreenContainer";
import { useJobPolling } from "../../components/job/JobPollingContext";
import { useTheme, useThemedStyles } from "../../components/ui/ThemeContext";
import type { JobStatus } from "../../types";

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
  const styles = useThemedStyles(() => ({
    center: { flex: 1, alignItems: "center" as const, justifyContent: "center" as const, gap: 12, padding: 24 },
    status: { marginTop: 8 },
    message: { textAlign: "center" as const },
    backButton: { marginTop: 16 },
  }));
  const { getJob, startTracking, dismiss } = useJobPolling();

  // 폴링 자체는 JobPollingProvider(앱 루트)가 소유한다 — 이 화면은 그 상태를 구독만 하고, 이 화면을
  // 벗어나도(뒤로가기 등) 추적은 끊기지 않는다.
  useEffect(() => {
    startTracking(jobId, redirectTo);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [jobId]);

  const job = getJob(jobId);
  const status = job?.status ?? "PENDING";
  const errorMessage = job?.errorMessage ?? null;

  useEffect(() => {
    if (job?.status === "COMPLETED") {
      dismiss(jobId);
      router.replace((redirectTo ?? "/") as Href);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [job?.status]);

  return (
    <ScreenContainer scroll={false}>
      <View style={styles.center}>
        {status === "FAILED" || errorMessage ? (
          <>
            <Text style={[typography.heading, { color: colors.danger }]}>{STATUS_LABEL.FAILED}</Text>
            <Text style={[typography.muted, styles.message]}>{errorMessage ?? "잠시 후 다시 시도해주세요."}</Text>
            <Button
              label="워크스페이스로 돌아가기"
              onPress={() => {
                dismiss(jobId);
                router.replace((redirectTo ?? "/") as Href);
              }}
              style={styles.backButton}
            />
          </>
        ) : (
          <>
            <ActivityIndicator size="large" color={colors.primary} />
            <Text style={[typography.heading, styles.status, { color: colors.primary }]}>{STATUS_LABEL[status]}</Text>
            <Text style={typography.muted}>보통 수십 초 정도 걸려요. 화면을 벗어나도 계속 진행되고, 완료되면 알려드려요.</Text>
          </>
        )}
      </View>
    </ScreenContainer>
  );
}
