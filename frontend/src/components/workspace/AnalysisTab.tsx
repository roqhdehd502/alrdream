import { useEffect, useState } from "react";
import { Text, View } from "react-native";
import { useRouter } from "expo-router";
import { analysisApi } from "../../api/analysis";
import { surveysApi } from "../../api/surveys";
import { designApi } from "../../api/design";
import { ApiError } from "../../api/client";
import { Button } from "../ui/Button";
import { EmptyState, ErrorBanner, Loading } from "../ui/Feedback";
import { useTheme, useThemedStyles } from "../ui/ThemeContext";
import { VersionList } from "./VersionList";
import { StatusBadge } from "./StatusBadge";
import { PdfButton } from "./PdfButton";
import { AnalysisContentView } from "./AnalysisContentView";
import { SurveyForm } from "../survey/SurveyForm";
import type { AnalysisVersionDetail, AnalysisVersionSummary, SurveyAnswer, SurveyDefinition } from "../../types";

export function AnalysisTab({ workspaceId, planningVersionId }: { workspaceId: string; planningVersionId: string | null }) {
  const router = useRouter();
  const { typography } = useTheme();
  const styles = useThemedStyles((colors) => ({
    wrap: { gap: 16 },
    detailHeader: { flexDirection: "row" as const, alignItems: "center" as const, justifyContent: "space-between" as const },
    backButton: { alignSelf: "flex-start" as const },
    actions: { gap: 10 },
    deleteLink: { alignSelf: "flex-start" as const },
    newButton: { alignSelf: "flex-start" as const },
    confirmRow: { gap: 10, backgroundColor: colors.dangerSoft, padding: 14, borderRadius: 12 },
    confirmButtons: { flexDirection: "row" as const, gap: 10 },
  }));
  const [versions, setVersions] = useState<AnalysisVersionSummary[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [selected, setSelected] = useState<AnalysisVersionSummary | null>(null);
  const [detail, setDetail] = useState<AnalysisVersionDetail | null>(null);
  const [confirmingDelete, setConfirmingDelete] = useState(false);
  const [busy, setBusy] = useState(false);

  const [startingDesign, setStartingDesign] = useState(false);
  const [designDefinition, setDesignDefinition] = useState<SurveyDefinition | null>(null);

  const reload = async () => {
    if (!planningVersionId) return;
    setVersions(null);
    try {
      setVersions(await analysisApi.list(workspaceId, planningVersionId));
    } catch (e) {
      setError(e instanceof ApiError ? e.message : String(e));
    }
  };

  useEffect(() => {
    if (!planningVersionId) return;
    const load = async () => {
      setVersions(null);
      try {
        setVersions(await analysisApi.list(workspaceId, planningVersionId));
      } catch (e) {
        setError(e instanceof ApiError ? e.message : String(e));
      }
    };
    load();
  }, [workspaceId, planningVersionId]);

  useEffect(() => {
    if (!selected || !planningVersionId) return;
    const load = async () => {
      setDetail(null);
      try {
        setDetail(await analysisApi.get(workspaceId, planningVersionId, selected.id));
      } catch (e) {
        setError(e instanceof ApiError ? e.message : String(e));
      }
    };
    load();
  }, [workspaceId, planningVersionId, selected]);

  const runAnalysis = async () => {
    if (!planningVersionId) return;
    setBusy(true);
    setError(null);
    try {
      const job = await analysisApi.create(workspaceId, planningVersionId);
      router.push({
        pathname: "/generating",
        params: { jobId: job.id, redirectTo: `/workspaces/${workspaceId}?tab=analysis` },
      });
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "분석 생성에 실패했습니다.");
      setBusy(false);
    }
  };

  const handleDelete = async () => {
    if (!selected || !planningVersionId) return;
    setBusy(true);
    try {
      await analysisApi.remove(workspaceId, planningVersionId, [selected.id]);
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

  const openDesignSurvey = async () => {
    setBusy(true);
    setError(null);
    try {
      const definition = await surveysApi.get(workspaceId, "DESIGN");
      setDesignDefinition(definition);
      setStartingDesign(true);
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "설문을 불러오지 못했습니다.");
    } finally {
      setBusy(false);
    }
  };

  const submitDesignSurvey = async (answers: SurveyAnswer[]) => {
    if (!selected || !planningVersionId) return;
    setBusy(true);
    setError(null);
    try {
      const response = await surveysApi.submit(workspaceId, "DESIGN", answers);
      const job = await designApi.create(workspaceId, planningVersionId, selected.id, response.id);
      router.push({
        pathname: "/generating",
        params: { jobId: job.id, redirectTo: `/workspaces/${workspaceId}?tab=design` },
      });
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "제출에 실패했습니다.");
      setBusy(false);
    }
  };

  if (!planningVersionId) {
    return (
      <View style={styles.wrap}>
        <EmptyState label="먼저 기획을 완료해야 분석을 시작할 수 있습니다." />
      </View>
    );
  }

  if (startingDesign && designDefinition) {
    return (
      <View style={styles.wrap}>
        <Text style={typography.title}>{designDefinition.title}</Text>
        <ErrorBanner message={error} />
        <SurveyForm
          definition={designDefinition}
          submitting={busy}
          submitLabel="설계 시작"
          onSubmit={submitDesignSurvey}
        />
        <Button label="취소" variant="ghost" onPress={() => setStartingDesign(false)} />
      </View>
    );
  }

  if (selected) {
    return (
      <View style={styles.wrap}>
        <Button label="← 목록으로" variant="ghost" onPress={() => setSelected(null)} style={styles.backButton} />
        <View style={styles.detailHeader}>
          <Text style={typography.title}>분석 v{selected.versionNo}</Text>
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
          detail.content && <AnalysisContentView content={detail.content} />
        )}

        {detail?.status === "COMPLETED" && (
          <View style={styles.actions}>
            <PdfButton onGenerate={() => analysisApi.generatePdf(workspaceId, planningVersionId, selected.id)} />
            <Button label="이 분석으로 설계 시작" onPress={openDesignSurvey} loading={busy} />
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
      <Button label="새로 분석하기" onPress={runAnalysis} loading={busy} style={styles.newButton} />
      <ErrorBanner message={error} />
      {versions === null ? (
        <Loading />
      ) : versions.length === 0 ? (
        <EmptyState label="아직 분석 결과가 없습니다." />
      ) : (
        <VersionList versions={versions} onSelect={setSelected} />
      )}
    </View>
  );
}
