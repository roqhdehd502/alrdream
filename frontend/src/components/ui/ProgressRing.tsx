import { Text, View } from "react-native";
import Svg, { Circle } from "react-native-svg";
import { useTheme } from "./ThemeContext";
import { fontFamily } from "./theme";

/** SVG 기반 원형 진행률 링 — account.tsx의 AI 생성 사용량 등 실제 0~1 비율이 있는 곳에 쓴다(Phase 20). */
export function ProgressRing({
  progress,
  size = 64,
  strokeWidth = 6,
  label,
}: {
  /** 0~1 사이 비율. 범위를 벗어나면 안쪽에서 clamp한다. */
  progress: number;
  size?: number;
  strokeWidth?: number;
  label?: string;
}) {
  const { colors } = useTheme();
  // progress가 NaN(예: 분모 0)이면 clamp를 거쳐도 NaN이 그대로 남아 strokeDashoffset이 깨진다(Phase 21
  // 전수 점검) — 호출부 방어와 별개로 컴포넌트 자체가 재사용 시에도 안전하도록 여기서 0으로 되돌린다.
  const clamped = Number.isFinite(progress) ? Math.max(0, Math.min(1, progress)) : 0;
  const radius = (size - strokeWidth) / 2;
  const circumference = 2 * Math.PI * radius;
  const dashOffset = circumference * (1 - clamped);

  return (
    <View style={{ width: size, height: size, alignItems: "center", justifyContent: "center" }}>
      <Svg width={size} height={size}>
        <Circle cx={size / 2} cy={size / 2} r={radius} stroke={colors.border} strokeWidth={strokeWidth} fill="none" />
        <Circle
          cx={size / 2}
          cy={size / 2}
          r={radius}
          stroke={colors.primary}
          strokeWidth={strokeWidth}
          fill="none"
          strokeDasharray={`${circumference} ${circumference}`}
          strokeDashoffset={dashOffset}
          strokeLinecap="round"
          transform={`rotate(-90 ${size / 2} ${size / 2})`}
        />
      </Svg>
      {label ? (
        <View style={{ position: "absolute" }}>
          <Text style={{ fontFamily: fontFamily.bold, fontSize: Math.round(size * 0.22), color: colors.text }}>
            {label}
          </Text>
        </View>
      ) : null}
    </View>
  );
}
