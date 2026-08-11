import { Pressable, Text, View } from "react-native";
import { Stack, useRouter } from "expo-router";
import { useTheme } from "../../components/ui/ThemeContext";
import { ThemeMenuButton } from "../../components/ui/ThemeMenuButton";
import { fontFamily } from "../../components/ui/theme";

function AccountButton() {
  const router = useRouter();
  const { colors } = useTheme();
  return (
    <View style={{ flexDirection: "row", alignItems: "center", gap: 14, paddingRight: 16 }}>
      <ThemeMenuButton />
      <Pressable onPress={() => router.push("/account")} hitSlop={10}>
        <Text style={{ color: colors.primary, fontSize: 14, fontFamily: fontFamily.semibold }}>계정</Text>
      </Pressable>
    </View>
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
      <Stack.Screen name="index" options={{ title: "내 워크스페이스", headerRight: AccountButton }} />
      <Stack.Screen name="account" options={{ title: "계정" }} />
      <Stack.Screen name="workspaces/new" options={{ title: "새 워크스페이스" }} />
      <Stack.Screen name="workspaces/[id]" options={{ title: "워크스페이스" }} />
      <Stack.Screen name="generating" options={{ title: "생성 중", headerBackVisible: false, gestureEnabled: false }} />
    </Stack>
  );
}
