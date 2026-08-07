import { ActivityIndicator, Text, View } from "react-native";
import { useTheme, useThemedStyles } from "./ThemeContext";
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

export function EmptyState({ label }: { label: string }) {
  const { typography } = useTheme();
  const styles = useThemedStyles(() => ({
    center: { paddingVertical: 40, alignItems: "center", justifyContent: "center" },
  }));
  return (
    <View style={styles.center}>
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
