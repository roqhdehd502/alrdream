import { Pressable, Text, View, type StyleProp, type ViewStyle } from "react-native";
import { useThemedStyles } from "./ThemeContext";
import { fontFamily } from "./theme";

/** 필 모양 세그먼트 탭 — 워크스페이스 상세의 기획/분석/설계/설정 탭 등 화면 안 탭 전환에 쓴다(Phase 20). */
export function PillTabs<T extends string>({
  items,
  value,
  onChange,
  style,
}: {
  items: { key: T; label: string }[];
  value: T;
  onChange: (key: T) => void;
  style?: StyleProp<ViewStyle>;
}) {
  const styles = useThemedStyles((colors) => ({
    row: {
      flexDirection: "row" as const,
      gap: 4,
      backgroundColor: colors.bg,
      padding: 4,
      borderRadius: 999,
      alignSelf: "flex-start" as const,
    },
    pill: { paddingVertical: 8, paddingHorizontal: 16, borderRadius: 999 },
    pillActive: { backgroundColor: colors.primary },
    labelActive: { color: "#fff", fontFamily: fontFamily.semibold, fontSize: 13 },
    labelInactive: { color: colors.textMuted, fontFamily: fontFamily.medium, fontSize: 13 },
  }));

  return (
    <View style={[styles.row, style]} accessibilityRole="tablist">
      {items.map((item) => {
        const active = item.key === value;
        return (
          <Pressable
            key={item.key}
            onPress={() => onChange(item.key)}
            style={[styles.pill, active && styles.pillActive]}
            accessibilityRole="tab"
            accessibilityState={{ selected: active }}
            accessibilityLabel={item.label}
          >
            <Text style={active ? styles.labelActive : styles.labelInactive}>{item.label}</Text>
          </Pressable>
        );
      })}
    </View>
  );
}
