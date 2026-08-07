import { ScrollView, StyleSheet, View } from "react-native";
import { useBreakpoint } from "./useBreakpoint";
import { colors } from "./theme";

const MAX_WIDTH = { mobile: undefined, tablet: 720, desktop: 960 } as const;
const H_PADDING = { mobile: 16, tablet: 32, desktop: 40 } as const;

/** 데스크톱/태블릿에서는 콘텐츠 폭을 제한하고 중앙 정렬, 모바일에서는 풀폭으로 쓰는 공용 화면 컨테이너. */
export function ScreenContainer({
  children,
  scroll = true,
}: {
  children: React.ReactNode;
  scroll?: boolean;
}) {
  const bp = useBreakpoint();
  const inner = (
    <View
      style={[
        styles.inner,
        { maxWidth: MAX_WIDTH[bp], paddingHorizontal: H_PADDING[bp] },
        !scroll && styles.innerFlex,
      ]}
    >
      {children}
    </View>
  );

  if (!scroll) {
    return <View style={styles.root}>{inner}</View>;
  }
  return (
    <ScrollView style={styles.root} contentContainerStyle={styles.scrollContent}>
      {inner}
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1, backgroundColor: colors.bg },
  scrollContent: { flexGrow: 1, alignItems: "stretch" },
  inner: { width: "100%", alignSelf: "center", paddingVertical: 24, gap: 20 },
  innerFlex: { flex: 1 },
});
