import { View, type StyleProp, type ViewStyle } from "react-native";
import { useThemedStyles } from "./ThemeContext";
import { radius, shadows, spacing } from "./theme";

type Tone = "default" | "danger";

export function Card({
  children,
  tone = "default",
  style,
}: {
  children: React.ReactNode;
  tone?: Tone;
  style?: StyleProp<ViewStyle>;
}) {
  const styles = useThemedStyles((colors) => ({
    card: {
      backgroundColor: colors.surface,
      borderWidth: 1,
      borderColor: colors.border,
      borderRadius: radius.md,
      padding: spacing(4),
      gap: spacing(2),
      ...shadows.sm,
    },
    danger: {
      backgroundColor: colors.dangerSoft,
      borderColor: colors.dangerSoft,
      shadowOpacity: 0,
      elevation: 0,
    },
  }));
  return <View style={[styles.card, tone === "danger" && styles.danger, style]}>{children}</View>;
}
