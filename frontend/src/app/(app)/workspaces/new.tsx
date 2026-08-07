import { useState } from "react";
import { Pressable, StyleSheet, Text, View } from "react-native";
import { useRouter } from "expo-router";
import { workspacesApi } from "../../../api/workspaces";
import { surveysApi } from "../../../api/surveys";
import { planningApi } from "../../../api/planning";
import { ApiError } from "../../../api/client";
import { ScreenContainer } from "../../../components/ui/ScreenContainer";
import { Button } from "../../../components/ui/Button";
import { Card } from "../../../components/ui/Card";
import { Field } from "../../../components/ui/Field";
import { ErrorBanner, Loading } from "../../../components/ui/Feedback";
import { colors, typography } from "../../../components/ui/theme";
import { SurveyForm } from "../../../components/survey/SurveyForm";
import type { SurveyAnswer, SurveyDefinition, SurveyKey } from "../../../types";

const BRANCHES: { key: SurveyKey; title: string; description: string }[] = [
  { key: "PLANNING_HAS_IDEA", title: "생각해둔 사업 아이템이 있어요", description: "아이템을 구체화하는 설문으로 시작합니다." },
  { key: "PLANNING_EXPLORING", title: "사업 아이템을 고민 중이에요", description: "관심 분야를 탐색하는 설문으로 시작합니다." },
];

export default function NewWorkspaceScreen() {
  const router = useRouter();
  const [step, setStep] = useState<"form" | "survey">("form");
  const [name, setName] = useState("");
  const [branch, setBranch] = useState<SurveyKey | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [creating, setCreating] = useState(false);
  const [workspaceId, setWorkspaceId] = useState<string | null>(null);
  const [definition, setDefinition] = useState<SurveyDefinition | null>(null);
  const [submittingSurvey, setSubmittingSurvey] = useState(false);

  const startSurvey = async () => {
    if (!name.trim() || !branch) return;
    setError(null);
    setCreating(true);
    try {
      const workspace = await workspacesApi.create(name.trim());
      const survey = await surveysApi.get(workspace.id, branch);
      setWorkspaceId(workspace.id);
      setDefinition(survey);
      setStep("survey");
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "워크스페이스 생성에 실패했습니다.");
    } finally {
      setCreating(false);
    }
  };

  const handleSurveySubmit = async (answers: SurveyAnswer[]) => {
    if (!workspaceId || !branch) return;
    setError(null);
    setSubmittingSurvey(true);
    try {
      const surveyResponse = await surveysApi.submit(workspaceId, branch, answers);
      const job = await planningApi.create(workspaceId, surveyResponse.id);
      router.replace({
        pathname: "/generating",
        params: { jobId: job.id, workspaceId, redirectTo: `/workspaces/${workspaceId}` },
      });
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "설문 제출에 실패했습니다.");
      setSubmittingSurvey(false);
    }
  };

  if (step === "survey" && definition) {
    return (
      <ScreenContainer>
        <Text style={typography.title}>{definition.title}</Text>
        <ErrorBanner message={error} />
        <SurveyForm definition={definition} submitting={submittingSurvey} onSubmit={handleSurveySubmit} />
      </ScreenContainer>
    );
  }

  return (
    <ScreenContainer>
      <Text style={typography.title}>새 워크스페이스</Text>

      <Field label="워크스페이스 이름" placeholder="예: 내 첫 사업 아이템" value={name} onChangeText={setName} />

      <View style={styles.branches}>
        <Text style={typography.label}>생각해둔 사업 아이템이 있으신가요?</Text>
        {BRANCHES.map((b) => {
          const active = branch === b.key;
          return (
            <Pressable key={b.key} onPress={() => setBranch(b.key)}>
              <Card style={[styles.branchCard, active && styles.branchCardActive]}>
                <Text style={[typography.heading, active && styles.activeText]}>{b.title}</Text>
                <Text style={typography.muted}>{b.description}</Text>
              </Card>
            </Pressable>
          );
        })}
      </View>

      <ErrorBanner message={error} />

      {creating ? <Loading /> : <Button label="다음" onPress={startSurvey} disabled={!name.trim() || !branch} />}
    </ScreenContainer>
  );
}

const styles = StyleSheet.create({
  branches: { gap: 10 },
  branchCard: { borderColor: colors.border },
  branchCardActive: { borderColor: colors.primary, backgroundColor: colors.primarySoft },
  activeText: { color: colors.primaryHover },
});
