import { surveysApi } from "../../api/surveys";
import type { SurveyAnswer, SurveyDefinition } from "../../types";

/**
 * PlanningVersionDetail.surveyResponseId만으로는 원래 PLANNING_HAS_IDEA/PLANNING_EXPLORING 중 어느 설문으로
 * 제출됐는지 알 수 없다(SurveyResponseDetail에는 surveyKey가 없다) — "수정" 플로우에서 같은 설문으로 다시
 * 프리필하기 위해, 두 설문 정의를 모두 가져와 답변의 questionId 집합과 매칭해 역으로 추론한다.
 */
export async function inferPlanningDefinition(
  workspaceId: string,
  answers: SurveyAnswer[],
): Promise<SurveyDefinition> {
  const [hasIdea, exploring] = await Promise.all([
    surveysApi.get(workspaceId, "PLANNING_HAS_IDEA"),
    surveysApi.get(workspaceId, "PLANNING_EXPLORING"),
  ]);
  const answerIds = new Set(answers.map((a) => a.questionId));
  const hasIdeaIds = new Set(hasIdea.questions.map((q) => q.id));
  const matchesHasIdea = answerIds.size > 0 && [...answerIds].every((id) => hasIdeaIds.has(id));
  return matchesHasIdea ? hasIdea : exploring;
}
