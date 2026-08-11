import { Pressable, Text, View } from "react-native";
import { usePathname, useRouter, type Href } from "expo-router";
import { useJobPolling } from "./JobPollingContext";
import { useTheme, useThemedStyles } from "../ui/ThemeContext";
import { shadows } from "../ui/theme";

/**
 * Phase 16 — 생성 화면(/generating)을 벗어난 뒤 작업이 완료/실패됐을 때 전역으로 보여주는 배너.
 * /generating 화면 자체는 같은 정보를 이미 화면 전체로 보여주므로 거기서는 중복 노출하지 않는다.
 */
export function JobCompletionBanner() {
  const { job, dismiss } = useJobPolling();
  const router = useRouter();
  const pathname = usePathname();
  const { colors, typography } = useTheme();
  const styles = useThemedStyles((colors) => ({
    root: {
      position: "absolute" as const,
      left: 16,
      right: 16,
      bottom: 24,
      flexDirection: "row" as const,
      alignItems: "center" as const,
      gap: 10,
      padding: 14,
      borderRadius: 14,
      backgroundColor: colors.surface,
      borderWidth: 1,
      borderColor: colors.border,
      ...shadows.md,
    },
    text: { flex: 1 },
    closeButton: { padding: 4 },
  }));

  if (!job || pathname === "/generating") return null;
  if (job.status !== "COMPLETED" && job.status !== "FAILED") return null;

  const message = job.status === "COMPLETED" ? "생성이 완료됐어요 · 확인하기" : "생성에 실패했어요 · 확인하기";

  return (
    <View style={styles.root}>
      <Pressable
        style={styles.text}
        onPress={() => {
          const redirectTo = job.redirectTo;
          dismiss();
          router.push((redirectTo ?? "/") as Href);
        }}
      >
        <Text style={typography.body}>{message}</Text>
      </Pressable>
      <Pressable style={styles.closeButton} onPress={dismiss} hitSlop={10}>
        <Text style={{ color: colors.textMuted, fontSize: 16 }}>×</Text>
      </Pressable>
    </View>
  );
}
