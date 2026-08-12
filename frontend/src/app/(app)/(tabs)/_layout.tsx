import { Tabs } from "expo-router";
import { AccountIcon, HomeIcon, SubscriptionIcon, WorkspaceIcon } from "../../../components/ui/icons";
import { ThemeMenuButton } from "../../../components/ui/ThemeMenuButton";
import { useTheme } from "../../../components/ui/ThemeContext";
import { fontFamily, radius, shadows } from "../../../components/ui/theme";

export default function TabsLayout() {
  const { colors } = useTheme();
  return (
    <Tabs
      screenOptions={{
        // 헤더가 ScreenContainer의 본문 배경(colors.bg)과 이어 붙어 보이도록 같은 토큰을 쓴다 —
        // colors.surface를 쓰면 다크 모드에서 두 톤 차이가 뚜렷이 보이는 이음매가 생긴다.
        headerStyle: { backgroundColor: colors.bg },
        headerTitleStyle: { color: colors.text, fontSize: 16, fontFamily: fontFamily.bold },
        headerTintColor: colors.text,
        headerShadowVisible: false,
        headerRight: () => <ThemeMenuButton style={{ marginRight: 16 }} />,
        // 플로팅 탭바 주위 여백(marginHorizontal/marginBottom)은 탭바 자체가 아니라 이 scene 영역이
        // 채우므로, 여기도 명시적으로 맞춰주지 않으면 React Navigation 기본 배경(테마 무관 고정값)이 비친다.
        sceneStyle: { backgroundColor: colors.bg },
        tabBarActiveTintColor: colors.primary,
        tabBarInactiveTintColor: colors.textFaint,
        // 화면 가장자리에 여백을 두고 필 형태로 띄운다("앱다운" 하단 탭바 — Phase 20).
        tabBarStyle: {
          backgroundColor: colors.surface,
          borderTopWidth: 0,
          marginHorizontal: 16,
          marginBottom: 16,
          borderRadius: radius.pill,
          height: 64,
          ...shadows.lg,
        },
        tabBarLabelStyle: { fontFamily: fontFamily.medium, fontSize: 11 },
      }}
    >
      <Tabs.Screen
        name="index"
        options={{
          title: "홈",
          tabBarLabel: "홈",
          tabBarIcon: ({ color, size }) => <HomeIcon size={size} color={color as string} />,
        }}
      />
      <Tabs.Screen
        name="workspaces"
        options={{
          title: "워크스페이스",
          tabBarLabel: "워크스페이스",
          tabBarIcon: ({ color, size }) => <WorkspaceIcon size={size} color={color as string} />,
        }}
      />
      <Tabs.Screen
        name="subscription"
        options={{
          title: "구독",
          tabBarLabel: "구독",
          tabBarIcon: ({ color, size }) => <SubscriptionIcon size={size} color={color as string} />,
        }}
      />
      <Tabs.Screen
        name="account"
        options={{
          title: "마이페이지",
          tabBarLabel: "마이페이지",
          tabBarIcon: ({ color, size }) => <AccountIcon size={size} color={color as string} />,
        }}
      />
    </Tabs>
  );
}
