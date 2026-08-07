import { View } from "react-native";
import { Section, TextRow } from "./ContentSections";
import { Badge } from "../ui/Badge";
import type { AnalysisContent } from "../../types";

export function AnalysisContentView({ content }: { content: AnalysisContent }) {
  return (
    <View style={{ gap: 12 }}>
      <Section title="⚖️ 합법성">
        <TextRow label="합법 여부 및 근거" value={content.legality.is_legal} />
        <TextRow label="유의 사항" value={content.legality.considerations} />
        <TextRow label="필요한 인허가/신고" value={content.legality.required_licenses_or_permits} />
      </Section>

      <Section title="🧰 자원 확보 가능성">
        <TextRow label="물적 자원" value={content.resource_availability.material_resources} />
        <TextRow label="인적 자원" value={content.resource_availability.human_resources} />
        <TextRow label="종합 평가" value={content.resource_availability.overall_feasibility} />
      </Section>

      <Section title="🆚 경쟁 환경">
        <TextRow label="기존 경쟁/대체 서비스" value={content.competitive_landscape.existing_competitors} />
        <TextRow label="시장 포화도" value={content.competitive_landscape.market_saturation} />
        <TextRow label="경쟁 우위" value={content.competitive_landscape.competitive_advantage} />
      </Section>

      <Section title="🧩 핵심 기능 후보">
        <View style={{ flexDirection: "row", flexWrap: "wrap", gap: 8 }}>
          {content.core_feature_candidates.map((f) => (
            <Badge key={f.key} label={f.label} tone="primary" />
          ))}
        </View>
      </Section>

      <Section title="📝 종합 의견">
        <TextRow label="" value={content.overall_assessment} />
      </Section>
    </View>
  );
}
