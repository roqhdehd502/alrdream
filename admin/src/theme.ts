export type ThemePreference = "light" | "dark";

const STORAGE_KEY = "alrdream-admin-theme";

export function getTheme(): ThemePreference {
  return document.documentElement.getAttribute("data-theme") === "light" ? "light" : "dark";
}

export function setTheme(theme: ThemePreference) {
  document.documentElement.setAttribute("data-theme", theme);
  try {
    localStorage.setItem(STORAGE_KEY, theme);
  } catch {
    // 프라이빗 브라우징 등으로 localStorage 접근이 막혀도 화면 전환 자체는 계속 동작해야 한다.
  }
}

export function toggleTheme(): ThemePreference {
  const next: ThemePreference = getTheme() === "dark" ? "light" : "dark";
  setTheme(next);
  return next;
}
