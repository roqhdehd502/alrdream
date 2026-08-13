import { useEffect, useState } from "react";
import { Text, View } from "react-native";
import { useRouter } from "expo-router";
import { planningApi } from "../../api/planning";
import { surveysApi } from "../../api/surveys";
import { analysisApi } from "../../api/analysis";
import { ApiError } from "../../api/client";
import { Button } from "../ui/Button";
import { Card } from "../ui/Card";
import { EmptyState, ErrorBanner, Loading } from "../ui/Feedback";
import { useTheme, useThemedStyles } from "../ui/ThemeContext";
import { VersionList } from "./VersionList";
import { StatusBadge } from "./StatusBadge";
import { PdfButton } from "./PdfButton";
import { PlanningContentView } from "./PlanningContentView";
import { SurveyForm } from "../survey/SurveyForm";
import { VersionDiffView } from "./VersionDiffView";
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
  const { typography } = useTheme();
  const styles = useThemedStyles(() => ({
    wrap: { gap: 16 },
    detailHeader: { flexDirection: "row" as const, alignItems: "center" as const, justifyContent: "space-between" as const },
    backButton: { alignSelf: "flex-start" as const },
    actions: { gap: 10 },
    deleteLink: { alignSelf: "flex-start" as const },
    confirmRow: { gap: 10 },
    confirmButtons: { flexDirection: "row" as const, gap: 10 },
    listHeader: { flexDirection: "row" as const, justifyContent: "flex-end" as const },
    bulkBar: { flexDirection: "row" as const, alignItems: "center" as const, justifyContent: "space-between" as const, gap: 10 },
  }));
  const [selected, setSelected] = useState<PlanningVersionSummary | null>(null);
  const [bulkMode, setBulkMode] = useState(false);
  const [bulkSelectedIds, setBulkSelectedIds] = useState<Set<string>>(new Set());
  const [bulkError, setBulkError] = useState<string | null>(null);
  const [bulkConfirming, setBulkConfirming] = useState(false);
  const [detail, setDetail] = useState<PlanningVersionDetail | null>(null);
  const [detailError, setDetailError] = useState<string | null>(null);
  const [confirmingDelete, setConfirmingDelete] = useState(false);
  const [busy, setBusy] = useState(false);
  const [comparing, setComparing] = useState(false);
  const [previousDetail, setPreviousDetail] = useState<PlanningVersionDetail | null>(null);
  const [compareError, setCompareError] = useState<string | null>(null);

  const previousVersion = selected
    ? versions
        ?.filter((v) => v.versionNo < selected.versionNo)
        .sort((a, b) => b.versionNo - a.versionNo)[0]
    : undefined;

  const [editing, setEditing] = useState(false);
  const [editDefinition, setEditDefinition] = useState<SurveyDefinition | null>(null);
  const [editAnswers, setEditAnswers] = useState<SurveyAnswer[] | undefined>(undefined);

  useEffect(() => {
    if (!selected) return;
    const load = async () => {
      setDetail(null);
      setDetailError(null);
      setComparing(false);
      setPreviousDetail(null);
      try {
        setDetail(await planningApi.get(workspaceId, selected.id));
      } catch (e) {
        setDetailError(e instanceof ApiError ? e.message : String(e));
      }
    };
    load();
  }, [workspaceId, selected]);

  const toggleCompare = async () => {
    if (comparing) {
      setComparing(false);
      return;
    }
    if (!previousVersion) return;
    setComparing(true);
    if (previousDetail?.id !== previousVersion.id) {
      setCompareError(null);
      try {
        setPreviousDetail(await planningApi.get(workspaceId, previousVersion.id));
      } catch (e) {
        setCompareError(e instanceof ApiError ? e.message : "이전 버전을 불러오지 못했습니다.");
      }
    }
  };

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
      router.replace({ pathname: "/generating", params: { jobId: job.id, redirectTo: `/workspaces/${workspaceId}` } });
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
      router.replace({
        pathname: "/generating",
        params: { jobId: job.id, redirectTo: `/workspaces/${workspaceId}?tab=analysis` },
      });
    } catch (e) {
      setDetailError(e instanceof ApiError ? e.message : "분석 시작에 실패했습니다.");
      setBusy(false);
    }
  };

  const toggleBulkMode = () => {
    setBulkMode((prev) => !prev);
    setBulkSelectedIds(new Set());
    setBulkError(null);
    setBulkConfirming(false);
  };

  const toggleBulkSelect = (id: string) => {
    setBulkSelectedIds((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  };

  const handleBulkDelete = async () => {
    if (bulkSelectedIds.size === 0) return;
    setBusy(true);
    try {
      await planningApi.remove(workspaceId, Array.from(bulkSelectedIds));
      setBulkMode(false);
      setBulkSelectedIds(new Set());
      setBulkConfirming(false);
      onReload();
    } catch (e) {
      setBulkError(e instanceof ApiError ? e.message : "삭제에 실패했습니다.");
    } finally {
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
            {previousVersion && (
              <Button
                label={comparing ? "비교 닫기" : `v${previousVersion.versionNo}과 비교`}
                variant="secondary"
                onPress={toggleCompare}
              />
            )}
          </View>
        )}

        {comparing && (
          <>
            <ErrorBanner message={compareError} />
            {previousDetail === null ? (
              <Loading />
            ) : previousDetail.status !== "COMPLETED" ? (
              <EmptyState label="이전 버전이 완료 상태가 아니라 비교할 수 없습니다." />
            ) : (
              detail?.content &&
              previousVersion && (
                <VersionDiffView
                  beforeLabel={`v${previousVersion.versionNo}`}
                  afterLabel={`v${selected.versionNo}`}
                  before={previousDetail.content}
                  after={detail.content}
                />
              )
            )}
          </>
        )}

        {!confirmingDelete ? (
          <Button label="삭제" variant="ghost" onPress={() => setConfirmingDelete(true)} style={styles.deleteLink} />
        ) : (
          <Card tone="danger" style={styles.confirmRow}>
            <Text style={typography.muted}>정말 삭제할까요? 이 작업은 되돌릴 수 없습니다.</Text>
            <View style={styles.confirmButtons}>
              <Button label="취소" variant="secondary" onPress={() => setConfirmingDelete(false)} />
              <Button label="삭제" variant="danger" onPress={handleDelete} loading={busy} />
            </View>
          </Card>
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
        <>
          <View style={styles.listHeader}>
            <Button
              label={bulkMode ? "선택 취소" : "선택 삭제"}
              variant="ghost"
              onPress={toggleBulkMode}
            />
          </View>
          <VersionList
            versions={versions}
            onSelect={setSelected}
            selectable={bulkMode}
            selectedIds={bulkSelectedIds}
            onToggleSelect={toggleBulkSelect}
          />
          {bulkMode && (
            <>
              <ErrorBanner message={bulkError} />
              {!bulkConfirming ? (
                <View style={styles.bulkBar}>
                  <Text style={typography.muted}>{bulkSelectedIds.size}개 선택됨</Text>
                  <Button
                    label="삭제"
                    variant="danger"
                    disabled={bulkSelectedIds.size === 0}
                    onPress={() => setBulkConfirming(true)}
                  />
                </View>
              ) : (
                <Card tone="danger" style={styles.confirmRow}>
                  <Text style={typography.muted}>선택한 {bulkSelectedIds.size}개를 삭제할까요? 이 작업은 되돌릴 수 없습니다.</Text>
                  <View style={styles.confirmButtons}>
                    <Button label="취소" variant="secondary" onPress={() => setBulkConfirming(false)} />
                    <Button label="삭제" variant="danger" onPress={handleBulkDelete} loading={busy} />
                  </View>
                </Card>
              )}
            </>
          )}
        </>
      )}
    </View>
  );
}
