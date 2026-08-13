import { Stack } from "expo-router";
import { useTheme } from "../../components/ui/ThemeContext";
import { fontFamily } from "../../components/ui/theme";

export default function AppLayout() {
  const { colors } = useTheme();
  return (
    <Stack
      screenOptions={{
        // 헤더가 ScreenContainer의 본문 배경(colors.bg)과 이어 붙어 보이도록 같은 토큰을 쓴다 —
        // colors.surface를 쓰면 다크 모드에서 두 톤 차이가 뚜렷이 보이는 이음매가 생긴다.
        headerStyle: { backgroundColor: colors.bg },
        headerTitleStyle: { color: colors.text, fontSize: 16, fontFamily: fontFamily.bold },
        headerTintColor: colors.text,
        headerShadowVisible: false,
        // 화면 전환 중이나 헤더 바깥 영역에 React Navigation 기본 배경(테마 무관 고정값)이 비치지 않도록.
        contentStyle: { backgroundColor: colors.bg },
      }}
    >
      <Stack.Screen name="(tabs)" options={{ headerShown: false }} />
      <Stack.Screen name="workspaces/new" options={{ title: "새 워크스페이스" }} />
      <Stack.Screen name="workspaces/[id]" options={{ title: "워크스페이스" }} />
      <Stack.Screen name="subscription/payments" options={{ title: "결제 내역" }} />
      <Stack.Screen name="coupon" options={{ title: "쿠폰 등록" }} />
      {/* Phase 21 전수 점검 — 폴링은 JobPollingProvider(앱 루트)가 화면과 무관하게 계속 추적하므로(Phase 16),
          뒤로가기를 막을 이유가 없다. 예전엔 headerBackVisible/gestureEnabled를 꺼뒀는데, 화면 안내 문구
          ("화면을 벗어나도 계속 진행되고, 완료되면 알려드려요")와 실제 동작이 모순됐다 — 뒤로가기를 열어
          문구대로 동작하게 한다. */}
      <Stack.Screen name="generating" options={{ title: "생성 중" }} />
    </Stack>
  );
}
