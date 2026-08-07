import { View, type StyleProp, type ViewStyle } from "react-native";
import { useThemedStyles } from "./ThemeContext";
import { radius, spacing } from "./theme";

export function Card({ children, style }: { children: React.ReactNode; style?: StyleProp<ViewStyle> }) {
  const styles = useThemedStyles((colors) => ({
    card: {
      backgroundColor: colors.surface,
      borderWidth: 1,
      borderColor: colors.border,
      borderRadius: radius.md,
      padding: spacing(4),
      gap: spacing(2),
    },
  }));
  return <View style={[styles.card, style]}>{children}</View>;
}
