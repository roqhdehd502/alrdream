import { View } from "react-native";
import { Section, TextRow, ListRow, StageList } from "./ContentSections";
import type { PlanningContent } from "../../types";

export function PlanningContentView({ content }: { content: PlanningContent }) {
  return (
    <View style={{ gap: 12 }}>
      <Section title="📌 아이디어 요약">
        <TextRow label="한 줄 아이디어" value={content.idea_summary.one_line_pitch} />
        <TextRow label="해결하려는 문제" value={content.idea_summary.problem_to_solve} />
        <TextRow label="타겟 고객" value={content.idea_summary.target_customer} />
        <TextRow label="핵심 가치" value={content.idea_summary.core_value} />
      </Section>

      <Section title="🔥 문제 정의">
        <TextRow label="어떤 문제인가" value={content.problem_definition.what} />
        <TextRow label="왜 중요한가" value={content.problem_definition.why_important} />
        <TextRow label="발생 빈도" value={content.problem_definition.frequency} />
      </Section>

      <Section title="🎯 타겟 고객 분석">
        <TextRow label="주요 사용자" value={content.target_customer_analysis.main_users} />
        <TextRow label="사용자 특징" value={content.target_customer_analysis.characteristics} />
        <TextRow label="사용 시나리오" value={content.target_customer_analysis.usage_scenario} />
      </Section>

      <Section title="💡 솔루션 제안">
        <TextRow label="서비스 개요" value={content.solution_proposal.service_overview} />
        <ListRow label="핵심 기능" items={content.solution_proposal.core_features} />
        <TextRow label="사용자 흐름" value={content.solution_proposal.user_flow} />
      </Section>

      <Section title="🧩 서비스 구조">
        <TextRow label="플랫폼 형태" value={content.service_structure.platform_type} />
        <ListRow label="주요 기능 블록" items={content.service_structure.feature_blocks} />
      </Section>

      <Section title="🆚 경쟁/대체 분석">
        <TextRow label="기존 해결 방법" value={content.competitive_analysis.existing_alternatives} />
        <TextRow label="경쟁 서비스" value={content.competitive_analysis.competitors} />
        <TextRow label="차별점" value={content.competitive_analysis.differentiation} />
      </Section>

      <Section title="💰 수익 모델">
        <TextRow label="어떻게 돈을 버는지" value={content.revenue_model.how_to_earn} />
        <TextRow label="현실성" value={content.revenue_model.feasibility} />
      </Section>

      <Section title="🚀 MVP 전략">
        <ListRow label="최소 기능" items={content.mvp_strategy.minimum_features} />
        <TextRow label="1차 출시 범위" value={content.mvp_strategy.launch_scope} />
      </Section>

      <Section title="🛠 실행 로드맵">
        <StageList items={content.execution_roadmap} />
      </Section>

      <Section title="⚠️ 리스크 & 보완 포인트">
        <TextRow label="부족한 점" value={content.risks_and_improvements.weaknesses} />
        <TextRow label="위험 요소" value={content.risks_and_improvements.risks} />
        <TextRow label="추가 조사 필요 영역" value={content.risks_and_improvements.needs_further_research} />
      </Section>
    </View>
  );
}
