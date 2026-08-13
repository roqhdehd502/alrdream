import type { ReactNode } from "react";
import { View, type StyleProp, type ViewStyle } from "react-native";
import { useTheme } from "./ThemeContext";

type Tone = "primary" | "surface" | "muted";
type Shape = "square" | "circle";

const TONE_BG: Record<Tone, keyof ReturnType<typeof useTheme>["colors"]> = {
  primary: "primarySoft",
  surface: "surface",
  muted: "bg",
};

/**
 * 컬러 사각형(또는 원형) 안에 아이콘을 담는 공용 "칩" — 홈 허브 카드, Pro 플랜 카드, 빈 상태 등 곳곳에서
 * 제각각 크기/radius/배경으로 반복되던 패턴을 하나로 통합했다(Phase 20).
 */
export function IconChip({
  icon,
  size = 44,
  tone = "primary",
  shape = "square",
  style,
}: {
  icon: ReactNode;
  size?: number;
  tone?: Tone;
  shape?: Shape;
  style?: StyleProp<ViewStyle>;
}) {
  const { colors } = useTheme();
  const radius = shape === "circle" ? size / 2 : Math.round(size * 0.32);
  return (
    <View
      style={[
        {
          width: size,
          height: size,
          borderRadius: radius,
          backgroundColor: colors[TONE_BG[tone]],
          alignItems: "center",
          justifyContent: "center",
        },
        style,
      ]}
    >
      {icon}
    </View>
  );
}
