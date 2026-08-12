import { Text, View, type StyleProp, type ViewStyle } from "react-native";
import { fontFamily } from "./theme";

// 브랜드 primary/accent + 흰 글자 대비가 나오는 톤으로만 고른 고정 팔레트 — 라이트/다크와 무관하게 항상 같은
// 색으로 보이는 편이 "이 사람의 아바타 색"이라는 정체성에 더 맞는다고 판단해 theme의 semantic 색(danger 등,
// 각기 다른 의미가 있어 재사용하면 혼동을 준다)과는 별개로 둔다.
const PALETTE = ["#4F46E5", "#7C3AED", "#0891B2", "#0D9488", "#C026D3", "#DB2777", "#EA580C", "#4338CA"];

function hashSeed(seed: string): number {
  let hash = 0;
  for (let i = 0; i < seed.length; i++) {
    hash = (hash * 31 + seed.charCodeAt(i)) >>> 0;
  }
  return hash;
}

/** Boring Avatars 스타일 — 시드(보통 member.id, 이름이 바뀌어도 색이 안정적이도록)로 고른 배경색 + 표시 이름 첫 글자. */
export function Avatar({
  seed,
  label,
  size = 40,
  style,
}: {
  seed: string;
  label: string;
  size?: number;
  style?: StyleProp<ViewStyle>;
}) {
  const color = PALETTE[hashSeed(seed) % PALETTE.length];
  const initial = label.trim().charAt(0).toUpperCase() || "?";
  return (
    <View
      style={[
        {
          width: size,
          height: size,
          borderRadius: size / 2,
          backgroundColor: color,
          alignItems: "center",
          justifyContent: "center",
        },
        style,
      ]}
    >
      <Text style={{ color: "#fff", fontFamily: fontFamily.bold, fontSize: Math.round(size * 0.42) }}>{initial}</Text>
    </View>
  );
}
