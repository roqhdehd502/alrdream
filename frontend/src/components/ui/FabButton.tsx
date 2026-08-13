import type { ReactNode } from "react";
import { Pressable, type StyleProp, type ViewStyle } from "react-native";
import { useTheme } from "./ThemeContext";
import { shadows } from "./theme";

/** 원형 플로팅 액션 버튼 — 위치는 항상 절대 위치로 호출부가 `style`에 bottom/right를 넘겨 잡는다(Phase 20). */
export function FabButton({
  icon,
  onPress,
  size = 56,
  style,
  accessibilityLabel,
}: {
  icon: ReactNode;
  onPress: () => void;
  size?: number;
  style?: StyleProp<ViewStyle>;
  /** 아이콘만 있는 버튼이라 스크린리더가 읽을 텍스트가 없다 — 호출부가 반드시 넘겨야 한다(Phase 21 전수 점검). */
  accessibilityLabel: string;
}) {
  const { colors } = useTheme();
  return (
    <Pressable
      onPress={onPress}
      accessibilityRole="button"
      accessibilityLabel={accessibilityLabel}
      style={({ pressed }) => [
        {
          position: "absolute" as const,
          width: size,
          height: size,
          borderRadius: size / 2,
          backgroundColor: colors.primary,
          alignItems: "center" as const,
          justifyContent: "center" as const,
          opacity: pressed ? 0.85 : 1,
        },
        shadows.lg,
        style,
      ]}
    >
      {icon}
    </Pressable>
  );
}
