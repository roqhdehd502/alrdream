import { Stack } from "expo-router";
import { useTheme } from "../../components/ui/ThemeContext";
import { fontFamily } from "../../components/ui/theme";

export default function AppLayout() {
  const { colors } = useTheme();
  return (
    <Stack
      screenOptions={{
        headerStyle: { backgroundColor: colors.surface },
        headerTitleStyle: { color: colors.text, fontSize: 16, fontFamily: fontFamily.bold },
        headerTintColor: colors.text,
        headerShadowVisible: false,
      }}
    >
      <Stack.Screen name="(tabs)" options={{ headerShown: false }} />
      <Stack.Screen name="workspaces/new" options={{ title: "새 워크스페이스" }} />
      <Stack.Screen name="workspaces/[id]" options={{ title: "워크스페이스" }} />
      <Stack.Screen name="subscription/payments" options={{ title: "결제 내역" }} />
      <Stack.Screen name="coupon" options={{ title: "쿠폰 등록" }} />
      <Stack.Screen name="generating" options={{ title: "생성 중", headerBackVisible: false, gestureEnabled: false }} />
    </Stack>
  );
}
