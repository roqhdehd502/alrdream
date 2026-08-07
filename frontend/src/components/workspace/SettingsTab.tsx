import { useState } from "react";
import { StyleSheet, Text, View } from "react-native";
import { useRouter } from "expo-router";
import { workspacesApi } from "../../api/workspaces";
import { ApiError } from "../../api/client";
import { Button } from "../ui/Button";
import { Field } from "../ui/Field";
import { ErrorBanner } from "../ui/Feedback";
import { colors, typography } from "../ui/theme";
import type { Workspace } from "../../types";

export function SettingsTab({ workspace, onRenamed }: { workspace: Workspace; onRenamed: (w: Workspace) => void }) {
  const router = useRouter();
  const [name, setName] = useState(workspace.name);
  const [saving, setSaving] = useState(false);
  const [savedMessage, setSavedMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [confirmingDelete, setConfirmingDelete] = useState(false);
  const [deleting, setDeleting] = useState(false);

  const handleSave = async () => {
    if (!name.trim()) return;
    setSaving(true);
    setError(null);
    setSavedMessage(null);
    try {
      const updated = await workspacesApi.rename(workspace.id, name.trim());
      onRenamed(updated);
      setSavedMessage("저장되었습니다.");
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "저장에 실패했습니다.");
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async () => {
    setDeleting(true);
    setError(null);
    try {
      await workspacesApi.remove(workspace.id);
      router.replace("/");
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "삭제에 실패했습니다.");
      setDeleting(false);
    }
  };

  return (
    <View style={styles.wrap}>
      <View style={styles.section}>
        <Text style={typography.heading}>워크스페이스 이름</Text>
        <Field label="이름" value={name} onChangeText={setName} />
        <ErrorBanner message={error} />
        {savedMessage ? <Text style={typography.muted}>{savedMessage}</Text> : null}
        <Button label="저장" onPress={handleSave} loading={saving} disabled={!name.trim()} style={styles.saveButton} />
      </View>

      <View style={styles.dangerSection}>
        <Text style={[typography.heading, { color: colors.danger }]}>워크스페이스 삭제</Text>
        <Text style={typography.muted}>삭제하면 이 워크스페이스의 기획/분석/설계 내역에 더 이상 접근할 수 없습니다.</Text>
        {!confirmingDelete ? (
          <Button label="워크스페이스 삭제" variant="danger" onPress={() => setConfirmingDelete(true)} style={styles.saveButton} />
        ) : (
          <View style={styles.confirmButtons}>
            <Button label="취소" variant="secondary" onPress={() => setConfirmingDelete(false)} />
            <Button label="정말 삭제" variant="danger" onPress={handleDelete} loading={deleting} />
          </View>
        )}
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  wrap: { gap: 28 },
  section: { gap: 10 },
  saveButton: { alignSelf: "flex-start" },
  dangerSection: {
    gap: 10,
    borderWidth: 1,
    borderColor: colors.dangerSoft,
    backgroundColor: colors.dangerSoft,
    padding: 16,
    borderRadius: 14,
  },
  confirmButtons: { flexDirection: "row", gap: 10 },
});
