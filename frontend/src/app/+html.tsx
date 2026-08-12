import { ScrollViewStyleReset } from "expo-router/html";
import type { PropsWithChildren } from "react";

// ThemeContext.tsx의 STORAGE_KEY/resolveScheme과 정확히 같은 기준(라이트를 명시적으로 선호하지 않는 한
// 다크가 기본값)으로 페이지 배경을 하이드레이션 전에 미리 칠한다 — admin/index.html의 부트스트랩과 동일한
// 목적(다크 모드에서 상단에 라이트 배경이 잠깐 비치는 현상 방지). 이 컴포넌트 자체는 Node.js에서만 실행돼
// localStorage/document에 접근할 수 없으므로, 실제로 브라우저에서 실행될 코드는 문자열로 된 <script> 태그로
// 내려보낸다. <html> 태그만 칠한다 — 이 스크립트는 <head> 안에서 실행되는데 이 시점엔 <body>가 아직
// 파싱되기 전이라 document.body가 null이라(건드리면 조용히 실패한다), body는 배경을 따로 지정하지 않아
// 투명하게 두면 뒤에 있는 html의 배경이 그대로 비쳐 보인다.
const BOOTSTRAP_SCRIPT = `
(function () {
  try {
    var stored = localStorage.getItem("alrdream_theme_preference");
    var isLight =
      stored === "light" ||
      (stored !== "dark" && window.matchMedia && window.matchMedia("(prefers-color-scheme: light)").matches);
    if (isLight) {
      document.documentElement.style.backgroundColor = "#F8FAFC";
    }
  } catch (e) {}
})();
`;

export default function Root({ children }: PropsWithChildren) {
  return (
    <html lang="ko">
      <head>
        <meta charSet="utf-8" />
        <meta name="viewport" content="width=device-width, initial-scale=1" />
        <ScrollViewStyleReset />
        {/* 기본값(다크)을 정적 CSS로 먼저 칠하고, 스크립트는 라이트로 바꿔야 할 때만 덮어쓴다. */}
        <style dangerouslySetInnerHTML={{ __html: "html{background-color:#0F172A;}" }} />
        <script dangerouslySetInnerHTML={{ __html: BOOTSTRAP_SCRIPT }} />
      </head>
      <body>{children}</body>
    </html>
  );
}
