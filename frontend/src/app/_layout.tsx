import { Stack, SplashScreen } from "expo-router";
import { AuthProvider, useAuth } from "../auth/AuthContext";

SplashScreen.preventAutoHideAsync();

function SplashScreenController() {
  const { status } = useAuth();
  if (status !== "loading") {
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

export default function RootLayout() {
  return (
    <AuthProvider>
      <SplashScreenController />
      <RootNavigator />
    </AuthProvider>
  );
}
