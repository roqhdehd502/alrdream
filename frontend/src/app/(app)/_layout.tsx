import { Pressable, Text } from "react-native";
import { Stack } from "expo-router";
import { useAuth } from "../../auth/AuthContext";
import { useTheme } from "../../components/ui/ThemeContext";
import { fontFamily } from "../../components/ui/theme";

function LogoutButton() {
  const { logout } = useAuth();
  const { colors } = useTheme();
  return (
    <Pressable onPress={logout} hitSlop={10}>
      <Text style={{ color: colors.primary, fontSize: 14, fontFamily: fontFamily.semibold }}>로그아웃</Text>
    </Pressable>
  );
}

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
      <Stack.Screen name="index" options={{ title: "내 워크스페이스", headerRight: LogoutButton }} />
      <Stack.Screen name="workspaces/new" options={{ title: "새 워크스페이스" }} />
      <Stack.Screen name="workspaces/[id]" options={{ title: "워크스페이스" }} />
      <Stack.Screen name="generating" options={{ title: "생성 중", headerBackVisible: false, gestureEnabled: false }} />
    </Stack>
  );
}
