import { useFonts } from "expo-font";
import { Stack, SplashScreen } from "expo-router";
import { StatusBar } from "expo-status-bar";
import { AuthProvider, useAuth } from "../auth/AuthContext";
import { ThemeProvider, useTheme } from "../components/ui/ThemeContext";

SplashScreen.preventAutoHideAsync();

export const fonts = {
  "Pretendard-Regular": require("../../assets/fonts/Pretendard-Regular.ttf"),
  "Pretendard-Medium": require("../../assets/fonts/Pretendard-Medium.ttf"),
  "Pretendard-SemiBold": require("../../assets/fonts/Pretendard-SemiBold.ttf"),
  "Pretendard-Bold": require("../../assets/fonts/Pretendard-Bold.ttf"),
};

function SplashScreenController({ fontsReady }: { fontsReady: boolean }) {
  const { status } = useAuth();
  const { ready: themeReady } = useTheme();
  if (status !== "loading" && fontsReady && themeReady) {
    SplashScreen.hide();
  }
  return null;
}

function RootNavigator() {
  const { status } = useAuth();
  const authenticated = status === "authenticated";

  return (
    <Stack screenOptions={{ headerShown: false }}>
      <Stack.Protected guard={authenticated}>
        <Stack.Screen name="(app)" />
      </Stack.Protected>
      <Stack.Protected guard={!authenticated}>
        <Stack.Screen name="sign-in" />
        <Stack.Screen name="sign-up" />
      </Stack.Protected>
    </Stack>
  );
}

function AppShell({ fontsReady }: { fontsReady: boolean }) {
  const { scheme } = useTheme();
  return (
    <AuthProvider>
      <StatusBar style={scheme === "light" ? "dark" : "light"} />
      <SplashScreenController fontsReady={fontsReady} />
      <RootNavigator />
    </AuthProvider>
  );
}

export default function RootLayout() {
  const [fontsLoaded, fontsError] = useFonts(fonts);
  const fontsReady = fontsLoaded || !!fontsError;

  return (
    <ThemeProvider>
      <AppShell fontsReady={fontsReady} />
    </ThemeProvider>
  );
}
