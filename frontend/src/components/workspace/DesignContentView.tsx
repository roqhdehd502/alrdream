import { StyleSheet, Text, View } from "react-native";
import { Section, TextRow, ListRow, StageList } from "./ContentSections";
import { Badge } from "../ui/Badge";
import { colors, typography } from "../ui/theme";
import type { DesignContent } from "../../types";

export function DesignContentView({ content }: { content: DesignContent }) {
  return (
    <View style={{ gap: 12 }}>
      <Section title="🧩 기능 명세">
        <View style={{ gap: 12 }}>
          {content.feature_specification.map((f, i) => (
            <View key={i} style={styles.featureRow}>
              <View style={styles.featureHeader}>
                <Text style={typography.label}>{f.feature}</Text>
                <Badge label={f.priority} tone="primary" />
              </View>
              <Text style={typography.body}>{f.description}</Text>
            </View>
          ))}
        </View>
      </Section>

      <Section title="🚀 MVP 정의">
        <TextRow label="MVP 범위" value={content.mvp_definition.scope} />
        <TextRow label="이후로 미루는 항목" value={content.mvp_definition.excluded_for_later} />
      </Section>

      <Section title="🛠 기술 아키텍처">
        <TextRow label="권장 기술 스택" value={content.technical_architecture.recommended_stack} />
        <TextRow label="아키텍처 개요" value={content.technical_architecture.architecture_overview} />
        <TextRow label="반영된 제약사항" value={content.technical_architecture.constraints_reflected} />
      </Section>

      <Section title="📱 플랫폼 계획">
        <TextRow label="우선 출시 플랫폼" value={content.platform_plan.primary_platform} />
        <TextRow label="선정 이유" value={content.platform_plan.rationale} />
      </Section>

      <Section title="🔒 데이터/개인정보">
        <TextRow label="다루는 민감 데이터" value={content.data_and_privacy.sensitive_data_handled} />
        <TextRow label="보호 방안" value={content.data_and_privacy.protection_measures} />
      </Section>

      <Section title="🗂 시스템 구조">
        <ListRow label="화면/기능 블록" items={content.system_structure} />
      </Section>

      <Section title="📅 개발 계획">
        <StageList items={content.development_plan} />
      </Section>
    </View>
  );
}

const styles = StyleSheet.create({
  featureRow: { gap: 4, borderTopWidth: 1, borderTopColor: colors.border, paddingTop: 10 },
  featureHeader: { flexDirection: "row", justifyContent: "space-between", alignItems: "center" },
});
