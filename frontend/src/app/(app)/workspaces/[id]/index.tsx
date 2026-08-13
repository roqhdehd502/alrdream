import { useCallback, useEffect, useMemo, useState } from "react";
import { useLocalSearchParams, useNavigation } from "expo-router";
import { workspacesApi } from "../../../../api/workspaces";
import { planningApi } from "../../../../api/planning";
import { ApiError } from "../../../../api/client";
import { ScreenContainer } from "../../../../components/ui/ScreenContainer";
import { Loading, ErrorBanner } from "../../../../components/ui/Feedback";
import { PillTabs } from "../../../../components/ui/PillTabs";
import { PlanningTab } from "../../../../components/workspace/PlanningTab";
import { AnalysisTab } from "../../../../components/workspace/AnalysisTab";
import { DesignTab } from "../../../../components/workspace/DesignTab";
import { SettingsTab } from "../../../../components/workspace/SettingsTab";
import type { PlanningVersionSummary, Workspace } from "../../../../types";

type TabKey = "planning" | "analysis" | "design" | "settings";

const TABS: { key: TabKey; label: string }[] = [
  { key: "planning", label: "기획" },
  { key: "analysis", label: "분석" },
  { key: "design", label: "설계" },
  { key: "settings", label: "설정" },
];

export default function WorkspaceDetailScreen() {
  const { id, tab: initialTab, analysisVersionId } = useLocalSearchParams<{
    id: string;
    tab?: TabKey;
    analysisVersionId?: string;
  }>();
  const navigation = useNavigation();
  const [workspace, setWorkspace] = useState<Workspace | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [tab, setTab] = useState<TabKey>((initialTab as TabKey) ?? "planning");

  const [planningVersions, setPlanningVersions] = useState<PlanningVersionSummary[] | null>(null);
  const [planningLoading, setPlanningLoading] = useState(true);
  const [planningError, setPlanningError] = useState<string | null>(null);

  useEffect(() => {
    workspacesApi
      .get(id)
      .then(setWorkspace)
      .catch((e) => setError(e instanceof ApiError ? e.message : String(e)));
  }, [id]);

  useEffect(() => {
    if (workspace) navigation.setOptions({ title: workspace.name });
  }, [workspace, navigation]);

  const reloadPlanning = useCallback(async () => {
    setPlanningLoading(true);
    try {
      setPlanningVersions(await planningApi.list(id));
    } catch (e) {
      setPlanningError(e instanceof ApiError ? e.message : String(e));
    } finally {
      setPlanningLoading(false);
    }
  }, [id]);

  useEffect(() => {
    const load = async () => {
      setPlanningLoading(true);
      try {
        setPlanningVersions(await planningApi.list(id));
      } catch (e) {
        setPlanningError(e instanceof ApiError ? e.message : String(e));
      } finally {
        setPlanningLoading(false);
      }
    };
    load();
  }, [id]);

  const latestCompletedPlanningId = useMemo(() => {
    const completed = (planningVersions ?? [])
      .filter((v) => v.status === "COMPLETED")
      .sort((a, b) => b.versionNo - a.versionNo);
    return completed[0]?.id ?? null;
  }, [planningVersions]);

  if (!workspace) {
    return (
      <ScreenContainer>
        <ErrorBanner message={error} />
        {!error && <Loading />}
      </ScreenContainer>
    );
  }

  return (
    <ScreenContainer>
      <PillTabs items={TABS} value={tab} onChange={setTab} style={{ marginBottom: 16 }} />

      {tab === "planning" && (
        <PlanningTab
          workspaceId={id}
          versions={planningVersions}
          loading={planningLoading}
          error={planningError}
          onReload={reloadPlanning}
        />
      )}
      {tab === "analysis" && <AnalysisTab workspaceId={id} planningVersionId={latestCompletedPlanningId} />}
      {tab === "design" && (
        <DesignTab
          workspaceId={id}
          planningVersionId={latestCompletedPlanningId}
          preferredAnalysisVersionId={analysisVersionId ?? null}
        />
      )}
      {tab === "settings" && <SettingsTab workspace={workspace} onRenamed={setWorkspace} />}
    </ScreenContainer>
  );
}
