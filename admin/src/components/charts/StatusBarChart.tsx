import { Bar, BarChart, Cell, LabelList, ResponsiveContainer, XAxis } from "recharts";

export interface StatusBarDatum {
  label: string;
  value: number;
  color: string;
}

/**
 * 상태별 수치를 막대로 보여주는 작은 차트 — 각 막대가 곧 카테고리라 별도 범례 없이 x축 라벨이
 * 정체성을 전달한다(dataviz 스킬: "단일 시리즈는 범례 불필요"). 값은 막대 위에 직접 라벨링한다.
 */
export function StatusBarChart({ data, height = 180 }: { data: StatusBarDatum[]; height?: number }) {
  return (
    <ResponsiveContainer width="100%" height={height}>
      <BarChart data={data} margin={{ top: 22, right: 8, left: 8, bottom: 0 }}>
        <XAxis
          dataKey="label"
          axisLine={{ stroke: "var(--color-border)" }}
          tickLine={false}
          tick={{ fill: "var(--color-text-muted)", fontSize: 12 }}
        />
        <Bar dataKey="value" radius={[4, 4, 0, 0]} maxBarSize={40} isAnimationActive={false}>
          {data.map((d) => (
            <Cell key={d.label} fill={d.color} />
          ))}
          <LabelList
            dataKey="value"
            position="top"
            style={{ fill: "var(--color-text)", fontSize: 13, fontWeight: 700 }}
            formatter={(label: string | number | boolean | null | undefined) =>
              typeof label === "number" ? label.toLocaleString("ko-KR") : String(label ?? "")
            }
          />
        </Bar>
      </BarChart>
    </ResponsiveContainer>
  );
}
