import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from "react";
import { Platform, StyleSheet, useColorScheme as useSystemColorScheme, type ColorSchemeName } from "react-native";
import * as SecureStore from "expo-secure-store";
import { createTypography, darkColors, lightColors, type ColorScheme, type ThemeColors, type Typography } from "./theme";

export type ThemePreference = ColorScheme | "system";

// expo-secure-store는 web을 지원하지 않는다 — native는 SecureStore, web은 localStorage로 분기 (tokenStorage.ts와 동일 패턴).
const STORAGE_KEY = "alrdream_theme_preference";

async function loadPreference(): Promise<ThemePreference> {
  const raw =
    Platform.OS === "web"
      ? typeof localStorage !== "undefined"
        ? localStorage.getItem(STORAGE_KEY)
        : null
      : await SecureStore.getItemAsync(STORAGE_KEY);
  return raw === "light" || raw === "dark" ? raw : "system";
}

async function savePreference(preference: ThemePreference): Promise<void> {
  if (preference === "system") {
    if (Platform.OS === "web") {
      if (typeof localStorage !== "undefined") localStorage.removeItem(STORAGE_KEY);
      return;
    }
    await SecureStore.deleteItemAsync(STORAGE_KEY);
    return;
  }
  if (Platform.OS === "web") {
    if (typeof localStorage !== "undefined") localStorage.setItem(STORAGE_KEY, preference);
    return;
  }
  await SecureStore.setItemAsync(STORAGE_KEY, preference);
}

interface ThemeContextValue {
  scheme: ColorScheme;
  preference: ThemePreference;
  colors: ThemeColors;
  typography: Typography;
  ready: boolean;
  setPreference: (preference: ThemePreference) => void;
  toggleScheme: () => void;
}

const ThemeContext = createContext<ThemeContextValue | null>(null);

// 시스템이 명시적으로 라이트를 선호하지 않는 한 브랜드 기본값(다크)을 유지한다 — admin/index.html 부트스트랩과 동일한 기준.
function resolveScheme(preference: ThemePreference, systemScheme: ColorSchemeName): ColorScheme {
  if (preference !== "system") return preference;
  return systemScheme === "light" ? "light" : "dark";
}

export function ThemeProvider({ children }: { children: ReactNode }) {
  const systemScheme = useSystemColorScheme();
  const [preference, setPreferenceState] = useState<ThemePreference>("system");
  const [ready, setReady] = useState(false);

  useEffect(() => {
    let cancelled = false;
    loadPreference().then((stored) => {
      if (cancelled) return;
      setPreferenceState(stored);
      setReady(true);
    });
    return () => {
      cancelled = true;
    };
  }, []);

  const setPreference = (next: ThemePreference) => {
    setPreferenceState(next);
    savePreference(next);
  };

  const toggleScheme = () => {
    const current = resolveScheme(preference, systemScheme);
    setPreference(current === "dark" ? "light" : "dark");
  };

  const scheme = resolveScheme(preference, systemScheme);
  const colors = scheme === "light" ? lightColors : darkColors;
  const typography = useMemo(() => createTypography(colors), [colors]);

  const value = useMemo(
    () => ({ scheme, preference, colors, typography, ready, setPreference, toggleScheme }),
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [scheme, preference, colors, typography, ready],
  );

  return <ThemeContext.Provider value={value}>{children}</ThemeContext.Provider>;
}

export function useTheme(): ThemeContextValue {
  const ctx = useContext(ThemeContext);
  if (!ctx) throw new Error("useTheme must be used within a ThemeProvider");
  return ctx;
}

export function useThemedStyles<T extends StyleSheet.NamedStyles<T>>(factory: (colors: ThemeColors) => T): T {
  const { colors } = useTheme();
  // factory는 보통 렌더마다 새로 만들어지는 인라인 함수지만 colors만의 순수 함수이므로, colors가 바뀔 때만
  // StyleSheet.create를 다시 호출한다(그렇지 않으면 테마가 안 바뀌어도 매 렌더 재할당된다).
  // eslint-disable-next-line react-hooks/exhaustive-deps
  return useMemo(() => StyleSheet.create(factory(colors)), [colors]);
}
