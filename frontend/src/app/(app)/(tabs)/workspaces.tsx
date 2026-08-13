import { useCallback, useEffect, useState } from "react";
import { FlatList, Pressable, Text, View } from "react-native";
import { useFocusEffect, useRouter } from "expo-router";
import { workspacesApi } from "../../../api/workspaces";
import { ApiError } from "../../../api/client";
import { ScreenContainer } from "../../../components/ui/ScreenContainer";
import { Card } from "../../../components/ui/Card";
import { Field } from "../../../components/ui/Field";
import { EmptyState, ErrorBanner, Loading } from "../../../components/ui/Feedback";
import { FabButton } from "../../../components/ui/FabButton";
import { IconChip } from "../../../components/ui/IconChip";
import { PlusIcon, WorkspaceIcon } from "../../../components/ui/icons";
import { useTheme, useThemedStyles } from "../../../components/ui/ThemeContext";
import type { Workspace } from "../../../types";

function formatDate(value: string) {
  return new Date(value).toLocaleDateString("ko-KR");
}

export default function WorkspaceListScreen() {
  const router = useRouter();
  const { colors, typography } = useTheme();
  const styles = useThemedStyles((colors) => ({
    search: { marginBottom: 12 },
    list: { gap: 12, paddingBottom: 24 },
    itemCard: { borderColor: colors.border },
    itemRow: { flexDirection: "row" as const, alignItems: "center" as const, gap: 14 },
    itemTextWrap: { flex: 1, gap: 2 },
  }));
  const [keyword, setKeyword] = useState("");
  const [workspaces, setWorkspaces] = useState<Workspace[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback((search?: string) => {
    setWorkspaces(null);
    workspacesApi
      .list(search || undefined, 0)
      .then((res) => setWorkspaces(res.content))
      .catch((e) => setError(e instanceof ApiError ? e.message : String(e)));
  }, []);

  useFocusEffect(
    useCallback(() => {
      load(keyword);
      // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []),
  );

  useEffect(() => {
    const timer = setTimeout(() => load(keyword), 300);
    return () => clearTimeout(timer);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [keyword]);

  return (
    <ScreenContainer scroll={false}>
      <Field
        placeholder="워크스페이스 검색"
        value={keyword}
        onChangeText={setKeyword}
        containerStyle={styles.search}
      />

      <ErrorBanner message={error} />

      {workspaces === null ? (
        <Loading />
      ) : workspaces.length === 0 ? (
        <EmptyState label="아직 워크스페이스가 없습니다. 새로 만들어보세요." />
      ) : (
        <FlatList
          data={workspaces}
          keyExtractor={(item) => item.id}
          contentContainerStyle={styles.list}
          renderItem={({ item }) => (
            <Pressable onPress={() => router.push(`/workspaces/${item.id}`)}>
              <Card style={styles.itemCard}>
                <View style={styles.itemRow}>
                  <IconChip icon={<WorkspaceIcon size={20} color={colors.primary} />} />
                  <View style={styles.itemTextWrap}>
                    <Text style={typography.heading}>{item.name}</Text>
                    <Text style={typography.muted}>수정일 {formatDate(item.updatedAt)}</Text>
                  </View>
                </View>
              </Card>
            </Pressable>
          )}
        />
      )}

      <FabButton
        icon={<PlusIcon size={22} color="#fff" />}
        onPress={() => router.push("/workspaces/new")}
        style={{ bottom: 24, right: 24 }}
        accessibilityLabel="새 워크스페이스 추가"
      />
    </ScreenContainer>
  );
}
