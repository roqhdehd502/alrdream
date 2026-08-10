/**
 * Phase 16 — 기획/분석/설계 각 content는 도메인마다 스키마가 다르고([01] 12-4 등 참고) 중첩된 객체/배열이
 * 섞여 있어, 도메인별로 비교 뷰를 따로 만드는 대신 값을 "경로: 문자열" 형태로 평탄화해 공통으로 비교한다.
 */

type FlatMap = Record<string, string>;

function flatten(value: unknown, prefix: string, out: FlatMap): void {
  if (value === null || value === undefined) {
    out[prefix] = "";
    return;
  }
  if (Array.isArray(value)) {
    if (value.length === 0) {
      out[prefix] = "(없음)";
      return;
    }
    value.forEach((item, idx) => flatten(item, `${prefix}[${idx + 1}]`, out));
    return;
  }
  if (typeof value === "object") {
    for (const [key, v] of Object.entries(value as Record<string, unknown>)) {
      flatten(v, prefix ? `${prefix}.${key}` : key, out);
    }
    return;
  }
  out[prefix] = String(value);
}

export interface DiffRow {
  path: string;
  before: string | null; // null = 이전 버전에는 없던 항목
  after: string | null; // null = 다음 버전에서 사라진 항목
}

export function diffContent(before: unknown, after: unknown): DiffRow[] {
  const beforeFlat: FlatMap = {};
  const afterFlat: FlatMap = {};
  flatten(before, "", beforeFlat);
  flatten(after, "", afterFlat);

  const paths = new Set([...Object.keys(beforeFlat), ...Object.keys(afterFlat)]);
  const rows: DiffRow[] = [];
  for (const path of paths) {
    const b = path in beforeFlat ? beforeFlat[path] : null;
    const a = path in afterFlat ? afterFlat[path] : null;
    if (b !== a) rows.push({ path, before: b, after: a });
  }
  return rows.sort((x, y) => x.path.localeCompare(y.path));
}

/** "idea_summary.one_line_pitch" → "idea summary > one line pitch" — 완벽한 라벨링 대신 읽기 쉬운 최소 가공. */
export function prettyPath(path: string): string {
  return path
    .replace(/\[(\d+)\]/g, " $1")
    .split(".")
    .map((segment) => segment.replace(/_/g, " "))
    .join(" > ");
}
