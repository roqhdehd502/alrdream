import { useEffect, useState } from "react";
import { StyleSheet, Text, View } from "react-native";
import { useRouter } from "expo-router";
import { planningApi } from "../../api/planning";
import { surveysApi } from "../../api/surveys";
import { analysisApi } from "../../api/analysis";
import { ApiError } from "../../api/client";
import { Button } from "../ui/Button";
import { EmptyState, ErrorBanner, Loading } from "../ui/Feedback";
import { colors, typography } from "../ui/theme";
import { VersionList } from "./VersionList";
import { StatusBadge } from "./StatusBadge";
import { PdfButton } from "./PdfButton";
import { PlanningContentView } from "./PlanningContentView";
import { SurveyForm } from "../survey/SurveyForm";
import { inferPlanningDefinition } from "./inferPlanningSurveyKey";
import type { PlanningVersionDetail, PlanningVersionSummary, SurveyAnswer, SurveyDefinition } from "../../types";

export function PlanningTab({
  workspaceId,
  versions,
  loading,
  error,
  onReload,
}: {
  workspaceId: string;
  versions: PlanningVersionSummary[] | null;
  loading: boolean;
  error: string | null;
  onReload: () => void;
}) {
  const router = useRouter();
  const [selected, setSelected] = useState<PlanningVersionSummary | null>(null);
  const [detail, setDetail] = useState<PlanningVersionDetail | null>(null);
  const [detailError, setDetailError] = useState<string | null>(null);
  const [confirmingDelete, setConfirmingDelete] = useState(false);
  const [busy, setBusy] = useState(false);

  const [editing, setEditing] = useState(false);
  const [editDefinition, setEditDefinition] = useState<SurveyDefinition | null>(null);
  const [editAnswers, setEditAnswers] = useState<SurveyAnswer[] | undefined>(undefined);

  useEffect(() => {
    if (!selected) return;
    const load = async () => {
      setDetail(null);
      setDetailError(null);
      try {
        setDetail(await planningApi.get(workspaceId, selected.id));
      } catch (e) {
        setDetailError(e instanceof ApiError ? e.message : String(e));
      }
    };
    load();
  }, [workspaceId, selected]);

  const startEdit = async () => {
    if (!detail) return;
    setBusy(true);
    setDetailError(null);
    try {
      const response = await surveysApi.getResponse(workspaceId, detail.surveyResponseId);
      const definition = await inferPlanningDefinition(workspaceId, response.answers);
      setEditDefinition(definition);
      setEditAnswers(response.answers);
      setEditing(true);
    } catch (e) {
      setDetailError(e instanceof ApiError ? e.message : "설문을 불러오지 못했습니다.");
    } finally {
      setBusy(false);
    }
  };

  const submitEdit = async (answers: SurveyAnswer[]) => {
    if (!editDefinition) return;
    setBusy(true);
    setDetailError(null);
    try {
      const response = await surveysApi.submit(workspaceId, editDefinition.surveyKey, answers);
      const job = await planningApi.create(workspaceId, response.id);
      router.push({ pathname: "/generating", params: { jobId: job.id, redirectTo: `/workspaces/${workspaceId}` } });
    } catch (e) {
      setDetailError(e instanceof ApiError ? e.message : "제출에 실패했습니다.");
      setBusy(false);
    }
  };

  const handleDelete = async () => {
    if (!selected) return;
    setBusy(true);
    try {
      await planningApi.remove(workspaceId, [selected.id]);
      setSelected(null);
      setDetail(null);
      setConfirmingDelete(false);
      onReload();
    } catch (e) {
      setDetailError(e instanceof ApiError ? e.message : "삭제에 실패했습니다.");
    } finally {
      setBusy(false);
    }
  };

  const startAnalysis = async () => {
    if (!selected) return;
    setBusy(true);
    setDetailError(null);
    try {
      const job = await analysisApi.create(workspaceId, selected.id);
      router.push({
        pathname: "/generating",
        params: { jobId: job.id, redirectTo: `/workspaces/${workspaceId}?tab=analysis` },
      });
    } catch (e) {
      setDetailError(e instanceof ApiError ? e.message : "분석 시작에 실패했습니다.");
      setBusy(false);
    }
  };

  if (editing && editDefinition) {
    return (
      <View style={styles.wrap}>
        <Text style={typography.title}>{editDefinition.title}</Text>
        <ErrorBanner message={detailError} />
        <SurveyForm
          definition={editDefinition}
          initialAnswers={editAnswers}
          submitting={busy}
          submitLabel="새 버전 생성"
          onSubmit={submitEdit}
        />
        <Button label="취소" variant="ghost" onPress={() => setEditing(false)} />
      </View>
    );
  }

  if (selected) {
    return (
      <View style={styles.wrap}>
        <Button label="← 목록으로" variant="ghost" onPress={() => setSelected(null)} style={styles.backButton} />
        <View style={styles.detailHeader}>
          <Text style={typography.title}>기획안 v{selected.versionNo}</Text>
          <StatusBadge status={selected.status} />
        </View>

        <ErrorBanner message={detailError} />

        {detail === null ? (
          <Loading />
        ) : detail.status === "GENERATING" ? (
          <EmptyState label="아직 생성 중입니다." />
        ) : detail.status === "FAILED" ? (
          <EmptyState label="생성에 실패한 버전입니다." />
        ) : (
          detail.content && <PlanningContentView content={detail.content} />
        )}

        {detail?.status === "COMPLETED" && (
          <View style={styles.actions}>
            <PdfButton onGenerate={() => planningApi.generatePdf(workspaceId, selected.id)} />
            <Button label="수정" variant="secondary" onPress={startEdit} loading={busy} />
            <Button label="이 기획으로 분석 시작" onPress={startAnalysis} loading={busy} />
          </View>
        )}

        {!confirmingDelete ? (
          <Button label="삭제" variant="ghost" onPress={() => setConfirmingDelete(true)} style={styles.deleteLink} />
        ) : (
          <View style={styles.confirmRow}>
            <Text style={typography.muted}>정말 삭제할까요? 이 작업은 되돌릴 수 없습니다.</Text>
            <View style={styles.confirmButtons}>
              <Button label="취소" variant="secondary" onPress={() => setConfirmingDelete(false)} />
              <Button label="삭제" variant="danger" onPress={handleDelete} loading={busy} />
            </View>
          </View>
        )}
      </View>
    );
  }

  return (
    <View style={styles.wrap}>
      <ErrorBanner message={error} />
      {loading ? (
        <Loading />
      ) : !versions || versions.length === 0 ? (
        <EmptyState label="아직 기획안이 없습니다." />
      ) : (
        <VersionList versions={versions} onSelect={setSelected} />
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  wrap: { gap: 16 },
  detailHeader: { flexDirection: "row", alignItems: "center", justifyContent: "space-between" },
  backButton: { alignSelf: "flex-start" },
  actions: { gap: 10 },
  deleteLink: { alignSelf: "flex-start" },
  confirmRow: {
    gap: 10,
    backgroundColor: colors.dangerSoft,
    padding: 14,
    borderRadius: 12,
  },
  confirmButtons: { flexDirection: "row", gap: 10 },
});
