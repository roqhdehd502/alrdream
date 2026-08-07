// Admin(admin/src/index.css)과 동일한 브랜드 팔레트(favicon 기준 purple/blue)를 공유한다.
export const colors = {
  primary: "#7c3aed",
  primaryHover: "#6d28d9",
  primarySoft: "#f1e8fe",
  accent: "#3aa0ff",
  bg: "#f6f5fa",
  surface: "#ffffff",
  border: "#e6e3ee",
  text: "#1c1730",
  textMuted: "#6b6580",
  textFaint: "#948fa3",
  danger: "#e0356b",
  dangerSoft: "#fdeaf0",
  success: "#1f9d6b",
  successSoft: "#e7f8f0",
  warning: "#b8720a",
  warningSoft: "#fdf1e0",
};

export const spacing = (n: number) => n * 4;

export const radius = {
  sm: 8,
  md: 12,
  lg: 18,
};

export const typography = {
  title: { fontSize: 24, fontWeight: "700" as const, color: colors.text },
  heading: { fontSize: 18, fontWeight: "700" as const, color: colors.text },
  body: { fontSize: 15, color: colors.text },
  muted: { fontSize: 13, color: colors.textMuted },
  label: { fontSize: 13, fontWeight: "600" as const, color: colors.text },
};
