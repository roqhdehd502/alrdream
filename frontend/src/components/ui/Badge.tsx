import { StyleSheet, Text, View } from "react-native";
import { colors } from "./theme";

type Tone = "neutral" | "primary" | "success" | "warning" | "danger";

const toneStyles: Record<Tone, { bg: string; fg: string }> = {
  neutral: { bg: "#eeecf3", fg: colors.textMuted },
  primary: { bg: colors.primarySoft, fg: colors.primaryHover },
  success: { bg: colors.successSoft, fg: colors.success },
  warning: { bg: colors.warningSoft, fg: colors.warning },
  danger: { bg: colors.dangerSoft, fg: colors.danger },
};

export function Badge({ label, tone = "neutral" }: { label: string; tone?: Tone }) {
  const t = toneStyles[tone];
  return (
    <View style={[styles.badge, { backgroundColor: t.bg }]}>
      <Text style={[styles.label, { color: t.fg }]}>{label}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  badge: { paddingVertical: 4, paddingHorizontal: 10, borderRadius: 999, alignSelf: "flex-start" },
  label: { fontSize: 12, fontWeight: "700" },
});
