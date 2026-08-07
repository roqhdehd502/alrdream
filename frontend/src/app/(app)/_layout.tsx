import { Pressable, Text } from "react-native";
import { Stack } from "expo-router";
import { useAuth } from "../../auth/AuthContext";
import { colors } from "../../components/ui/theme";

function LogoutButton() {
  const { logout } = useAuth();
  return (
    <Pressable onPress={logout} hitSlop={10}>
      <Text style={{ color: colors.primary, fontSize: 14, fontWeight: "600" }}>로그아웃</Text>
    </Pressable>
  );
}

export default function AppLayout() {
  return (
    <Stack
      screenOptions={{
        headerStyle: { backgroundColor: colors.surface },
        headerTitleStyle: { color: colors.text, fontSize: 16 },
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
