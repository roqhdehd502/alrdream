import { useFonts } from "expo-font";
import { DarkTheme, DefaultTheme, Stack, SplashScreen, ThemeProvider as NavigationThemeProvider } from "expo-router";
import { StatusBar } from "expo-status-bar";
import { useMemo } from "react";
import { AuthProvider, useAuth } from "../auth/AuthContext";
import { JobCompletionBanner } from "../components/job/JobCompletionBanner";
import { JobPollingProvider } from "../components/job/JobPollingContext";
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
        <Stack.Screen name="forgot-password" />
        <Stack.Screen name="reset-password" />
      </Stack.Protected>
    </Stack>
  );
}

function AppShell({ fontsReady }: { fontsReady: boolean }) {
  const { scheme, colors } = useTheme();
  // ExpoRoot은 NavigationContainer에 항상 react-navigation의 고정 DefaultTheme(라이트)를 쓴다 —
  // 우리 앱의 다크 모드와 무관하게 헤더/탭바가 명시적으로 칠하지 않은 여백(플로팅 탭바 마진, 화면 전환
  // 중 카드 배경 등)에 이 고정 배경이 그대로 비쳐 "테마와 이질적인 배경"으로 보인다. 여기서 실제 테마
  // 색으로 덮어써 react-navigation이 관리하는 모든 기본 배경을 우리 색상 체계와 일치시킨다.
  const navigationTheme = useMemo(() => {
    const base = scheme === "dark" ? DarkTheme : DefaultTheme;
    return {
      ...base,
      dark: scheme === "dark",
      colors: {
        ...base.colors,
        primary: colors.primary,
        background: colors.bg,
        card: colors.surface,
        text: colors.text,
        border: colors.border,
        notification: colors.danger,
      },
    };
  }, [scheme, colors]);

  return (
    <AuthProvider>
      <JobPollingProvider>
        <StatusBar style={scheme === "light" ? "dark" : "light"} />
        <SplashScreenController fontsReady={fontsReady} />
        <NavigationThemeProvider value={navigationTheme}>
          <RootNavigator />
        </NavigationThemeProvider>
        <JobCompletionBanner />
      </JobPollingProvider>
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
