import type { ReactNode } from "react";
import { ActivityIndicator, Text, View } from "react-native";
import { useTheme, useThemedStyles } from "./ThemeContext";
import { InboxIcon } from "./icons";
import { radius } from "./theme";

export function Loading() {
  const { colors } = useTheme();
  const styles = useThemedStyles(() => ({
    center: { paddingVertical: 40, alignItems: "center", justifyContent: "center" },
  }));
  return (
    <View style={styles.center}>
      <ActivityIndicator color={colors.primary} />
    </View>
  );
}

export function EmptyState({ label, icon }: { label: string; icon?: ReactNode }) {
  const { colors, typography } = useTheme();
  const styles = useThemedStyles((colors) => ({
    center: { paddingVertical: 40, alignItems: "center", justifyContent: "center", gap: 10 },
    iconWrap: {
      width: 40,
      height: 40,
      borderRadius: 999,
      backgroundColor: colors.bg,
      alignItems: "center",
      justifyContent: "center",
    },
  }));
  return (
    <View style={styles.center}>
      <View style={styles.iconWrap}>{icon ?? <InboxIcon size={20} color={colors.textFaint} />}</View>
      <Text style={typography.muted}>{label}</Text>
    </View>
  );
}

export function ErrorBanner({ message }: { message: string | null | undefined }) {
  const styles = useThemedStyles((colors) => ({
    errorBox: {
      backgroundColor: colors.dangerSoft,
      borderRadius: radius.sm,
      padding: 12,
    },
    errorText: { color: colors.danger, fontSize: 13.5 },
  }));
  if (!message) return null;
  return (
    <View style={styles.errorBox}>
      <Text style={styles.errorText}>{message}</Text>
    </View>
  );
}
