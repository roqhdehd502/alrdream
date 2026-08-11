import { Tabs } from "expo-router";
import { AccountIcon, HomeIcon, SubscriptionIcon, WorkspaceIcon } from "../../../components/ui/icons";
import { ThemeMenuButton } from "../../../components/ui/ThemeMenuButton";
import { useTheme } from "../../../components/ui/ThemeContext";
import { fontFamily } from "../../../components/ui/theme";

export default function TabsLayout() {
  const { colors } = useTheme();
  return (
    <Tabs
      screenOptions={{
        headerStyle: { backgroundColor: colors.surface },
        headerTitleStyle: { color: colors.text, fontSize: 16, fontFamily: fontFamily.bold },
        headerTintColor: colors.text,
        headerShadowVisible: false,
        headerRight: () => <ThemeMenuButton style={{ marginRight: 16 }} />,
        tabBarActiveTintColor: colors.primary,
        tabBarInactiveTintColor: colors.textFaint,
        tabBarStyle: { backgroundColor: colors.surface, borderTopColor: colors.border },
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
