import { useEffect, useState } from "react";
import { StyleSheet, Text, View } from "react-native";
import { useRouter } from "expo-router";
import { analysisApi } from "../../api/analysis";
import { designApi } from "../../api/design";
import { surveysApi } from "../../api/surveys";
import { ApiError } from "../../api/client";
import { Button } from "../ui/Button";
import { EmptyState, ErrorBanner, Loading } from "../ui/Feedback";
import { colors, typography } from "../ui/theme";
import { VersionList } from "./VersionList";
import { StatusBadge } from "./StatusBadge";
import { PdfButton } from "./PdfButton";
import { DesignContentView } from "./DesignContentView";
import { SurveyForm } from "../survey/SurveyForm";
import type { AnalysisVersionSummary, DesignVersionDetail, DesignVersionSummary, SurveyAnswer, SurveyDefinition } from "../../types";

export function DesignTab({ workspaceId, planningVersionId }: { workspaceId: string; planningVersionId: string | null }) {
  const router = useRouter();
  const [analysisVersion, setAnalysisVersion] = useState<AnalysisVersionSummary | null | undefined>(undefined);
  const [versions, setVersions] = useState<DesignVersionSummary[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [selected, setSelected] = useState<DesignVersionSummary | null>(null);
  const [detail, setDetail] = useState<DesignVersionDetail | null>(null);
  const [confirmingDelete, setConfirmingDelete] = useState(false);
  const [busy, setBusy] = useState(false);

  const [editing, setEditing] = useState(false);
  const [editDefinition, setEditDefinition] = useState<SurveyDefinition | null>(null);
  const [editAnswers, setEditAnswers] = useState<SurveyAnswer[] | undefined>(undefined);

  useEffect(() => {
    const load = async () => {
      if (!planningVersionId) {
        setAnalysisVersion(null);
        return;
      }
      try {
        const list = await analysisApi.list(workspaceId, planningVersionId);
        const completed = list.filter((v) => v.status === "COMPLETED").sort((a, b) => b.versionNo - a.versionNo);
        setAnalysisVersion(completed[0] ?? null);
      } catch (e) {
        setError(e instanceof ApiError ? e.message : String(e));
      }
    };
    load();
  }, [workspaceId, planningVersionId]);

  const reload = async () => {
    if (!planningVersionId || !analysisVersion) return;
    setVersions(null);
    try {
      setVersions(await designApi.list(workspaceId, planningVersionId, analysisVersion.id));
    } catch (e) {
      setError(e instanceof ApiError ? e.message : String(e));
    }
  };

  useEffect(() => {
    if (!planningVersionId || !analysisVersion) return;
    const load = async () => {
      setVersions(null);
      try {
        setVersions(await designApi.list(workspaceId, planningVersionId, analysisVersion.id));
      } catch (e) {
        setError(e instanceof ApiError ? e.message : String(e));
      }
    };
    load();
  }, [workspaceId, planningVersionId, analysisVersion]);

  useEffect(() => {
    if (!selected || !planningVersionId || !analysisVersion) return;
    const load = async () => {
      setDetail(null);
      try {
        setDetail(await designApi.get(workspaceId, planningVersionId, analysisVersion.id, selected.id));
      } catch (e) {
        setError(e instanceof ApiError ? e.message : String(e));
      }
    };
    load();
  }, [workspaceId, planningVersionId, analysisVersion, selected]);

  const startEdit = async () => {
    if (!detail) return;
    setBusy(true);
    setError(null);
    try {
      const [response, definition] = await Promise.all([
        surveysApi.getResponse(workspaceId, detail.surveyResponseId),
        surveysApi.get(workspaceId, "DESIGN"),
      ]);
      setEditDefinition(definition);
      setEditAnswers(response.answers);
      setEditing(true);
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "설문을 불러오지 못했습니다.");
    } finally {
      setBusy(false);
    }
  };

  const submitEdit = async (answers: SurveyAnswer[]) => {
    if (!planningVersionId || !analysisVersion) return;
    setBusy(true);
    setError(null);
    try {
      const response = await surveysApi.submit(workspaceId, "DESIGN", answers);
      const job = await designApi.create(workspaceId, planningVersionId, analysisVersion.id, response.id);
      router.push({
        pathname: "/generating",
        params: { jobId: job.id, redirectTo: `/workspaces/${workspaceId}?tab=design` },
      });
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "제출에 실패했습니다.");
      setBusy(false);
    }
  };

  const handleDelete = async () => {
    if (!selected || !planningVersionId || !analysisVersion) return;
    setBusy(true);
    try {
      await designApi.remove(workspaceId, planningVersionId, analysisVersion.id, [selected.id]);
      setSelected(null);
      setDetail(null);
      setConfirmingDelete(false);
      reload();
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "삭제에 실패했습니다.");
    } finally {
      setBusy(false);
    }
  };

  if (!planningVersionId || analysisVersion === null) {
    return (
      <View style={styles.wrap}>
        <EmptyState label="먼저 분석을 완료해야 설계를 시작할 수 있습니다." />
      </View>
    );
  }
  if (analysisVersion === undefined) {
    return <Loading />;
  }

  if (editing && editDefinition) {
    return (
      <View style={styles.wrap}>
        <Text style={typography.title}>{editDefinition.title}</Text>
        <ErrorBanner message={error} />
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
          <Text style={typography.title}>설계 v{selected.versionNo}</Text>
          <StatusBadge status={selected.status} />
        </View>

        <ErrorBanner message={error} />

        {detail === null ? (
          <Loading />
        ) : detail.status === "GENERATING" ? (
          <EmptyState label="아직 생성 중입니다." />
        ) : detail.status === "FAILED" ? (
          <EmptyState label="생성에 실패한 버전입니다." />
        ) : (
          detail.content && <DesignContentView content={detail.content} />
        )}

        {detail?.status === "COMPLETED" && (
          <View style={styles.actions}>
            <PdfButton
              onGenerate={() => designApi.generatePdf(workspaceId, planningVersionId, analysisVersion.id, selected.id)}
            />
            <Button label="수정" variant="secondary" onPress={startEdit} loading={busy} />
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
      {versions === null ? (
        <Loading />
      ) : versions.length === 0 ? (
        <EmptyState label="아직 설계 결과가 없습니다. 분석 탭에서 설계를 시작해보세요." />
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
  confirmRow: { gap: 10, backgroundColor: colors.dangerSoft, padding: 14, borderRadius: 12 },
  confirmButtons: { flexDirection: "row", gap: 10 },
});
