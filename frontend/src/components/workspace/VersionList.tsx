import { Pressable, Text, View } from "react-native";
import { Card } from "../ui/Card";
import { CheckIcon } from "../ui/icons";
import { StatusBadge } from "./StatusBadge";
import { useTheme, useThemedStyles } from "../ui/ThemeContext";
import type { VersionStatus } from "../../types";

interface VersionItem {
  id: string;
  versionNo: number;
  status: VersionStatus;
  createdAt: string;
}

export function VersionList<T extends VersionItem>({
  versions,
  onSelect,
  selectable = false,
  selectedIds,
  onToggleSelect,
}: {
  versions: T[];
  onSelect: (version: T) => void;
  /** true면 항목을 눌렀을 때 상세로 이동하는 대신 다중 선택 체크박스를 토글한다. [01] 6,9,11번 다중 선택 삭제. */
  selectable?: boolean;
  selectedIds?: Set<string>;
  onToggleSelect?: (id: string) => void;
}) {
  const { typography } = useTheme();
  const styles = useThemedStyles((colors) => ({
    list: { gap: 10 },
    item: { borderColor: colors.border },
    itemSelected: { borderColor: colors.primary },
    row: { flexDirection: "row" as const, alignItems: "center" as const, justifyContent: "space-between" as const },
    left: { flexDirection: "row" as const, alignItems: "center" as const, gap: 10 },
    checkbox: {
      width: 20,
      height: 20,
      borderRadius: 5,
      borderWidth: 1.5,
      borderColor: colors.border,
      alignItems: "center" as const,
      justifyContent: "center" as const,
    },
    checkboxChecked: { borderColor: colors.primary, backgroundColor: colors.primary },
  }));
  const sorted = [...versions].sort((a, b) => b.versionNo - a.versionNo);
  return (
    <View style={styles.list}>
      {sorted.map((v) => {
        const checked = selectedIds?.has(v.id) ?? false;
        return (
          <Pressable
            key={v.id}
            onPress={() => (selectable ? onToggleSelect?.(v.id) : onSelect(v))}
          >
            <Card style={[styles.item, selectable && checked ? styles.itemSelected : null]}>
              <View style={styles.row}>
                <View style={styles.left}>
                  {selectable && (
                    <View style={[styles.checkbox, checked ? styles.checkboxChecked : null]}>
                      {checked && <CheckIcon size={12} color="#fff" />}
                    </View>
                  )}
                  <Text style={typography.label}>v{v.versionNo}</Text>
                </View>
                <StatusBadge status={v.status} />
              </View>
              <Text style={typography.muted}>{new Date(v.createdAt).toLocaleString("ko-KR")}</Text>
            </Card>
          </Pressable>
        );
      })}
    </View>
  );
}
