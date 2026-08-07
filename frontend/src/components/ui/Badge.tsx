import { StyleSheet, Text, View } from "react-native";
import { useTheme } from "./ThemeContext";
import { fontFamily, type ThemeColors } from "./theme";

type Tone = "neutral" | "primary" | "success" | "warning" | "danger";

function toneStyle(colors: ThemeColors, tone: Tone): { bg: string; fg: string } {
  switch (tone) {
    case "primary":
      return { bg: colors.primarySoft, fg: colors.primaryHover };
    case "success":
      return { bg: colors.successSoft, fg: colors.success };
    case "warning":
      return { bg: colors.warningSoft, fg: colors.warning };
    case "danger":
      return { bg: colors.dangerSoft, fg: colors.danger };
    default:
      return { bg: colors.border, fg: colors.textMuted };
  }
}

export function Badge({ label, tone = "neutral" }: { label: string; tone?: Tone }) {
  const { colors } = useTheme();
  const t = toneStyle(colors, tone);
  return (
    <View style={[styles.badge, { backgroundColor: t.bg }]}>
      <Text style={[styles.label, { color: t.fg }]}>{label}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  badge: { paddingVertical: 4, paddingHorizontal: 10, borderRadius: 999, alignSelf: "flex-start" },
  label: { fontSize: 12, fontFamily: fontFamily.bold },
});
