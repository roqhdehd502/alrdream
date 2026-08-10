// Admin(admin/src/index.css)과 동일한 브랜드 팔레트(docs/05_color_and_font.md 기준 Indigo/Purple)를
// 라이트/다크 두 세트로 공유한다. 실제 사용 시점의 활성 팔레트는 ThemeContext의 useTheme()으로 가져온다.
export const darkColors = {
  primary: "#4F46E5",
  primaryHover: "#6366F1",
  primaryActive: "#4338CA",
  primarySoft: "rgba(99, 102, 241, 0.16)",
  accent: "#7C3AED",
  accentSoft: "rgba(124, 58, 237, 0.16)",
  bg: "#0F172A",
  surface: "#1E293B",
  border: "#334155",
  text: "#F1F5F9",
  textMuted: "#94A3B8",
  textFaint: "#64748B",
  danger: "#EF4444",
  dangerSoft: "rgba(239, 68, 68, 0.16)",
  success: "#22C55E",
  successSoft: "rgba(34, 197, 94, 0.16)",
  warning: "#F59E0B",
  warningSoft: "rgba(245, 158, 11, 0.16)",
};

export const lightColors = {
  primary: "#4F46E5",
  primaryHover: "#4338CA",
  primaryActive: "#3730A3",
  primarySoft: "#EEF2FF",
  accent: "#7C3AED",
  accentSoft: "#F3E8FF",
  bg: "#F8FAFC",
  surface: "#FFFFFF",
  border: "#E2E8F0",
  text: "#0F172A",
  textMuted: "#64748B",
  textFaint: "#94A3B8",
  danger: "#EF4444",
  dangerSoft: "#FEE2E2",
  success: "#22C55E",
  successSoft: "#DCFCE7",
  warning: "#F59E0B",
  warningSoft: "#FEF3C7",
};

export type ThemeColors = typeof darkColors;
export type ColorScheme = "light" | "dark";

export const spacing = (n: number) => n * 4;

export const radius = {
  sm: 8,
  md: 12,
  lg: 18,
};

export const fontFamily = {
  regular: "Pretendard-Regular",
  medium: "Pretendard-Medium",
  semibold: "Pretendard-SemiBold",
  bold: "Pretendard-Bold",
};

export function createTypography(colors: ThemeColors) {
  return {
    title: { fontSize: 24, fontFamily: fontFamily.bold, color: colors.text },
    heading: { fontSize: 18, fontFamily: fontFamily.bold, color: colors.text },
    body: { fontSize: 15, fontFamily: fontFamily.regular, color: colors.text },
    muted: { fontSize: 13, fontFamily: fontFamily.regular, color: colors.textMuted },
    label: { fontSize: 13, fontFamily: fontFamily.semibold, color: colors.text },
  };
}

export type Typography = ReturnType<typeof createTypography>;
