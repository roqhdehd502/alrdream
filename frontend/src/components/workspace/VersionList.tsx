import { Pressable, Text, View } from "react-native";
import { Card } from "../ui/Card";
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
}: {
  versions: T[];
  onSelect: (version: T) => void;
}) {
  const { typography } = useTheme();
  const styles = useThemedStyles((colors) => ({
    list: { gap: 10 },
    item: { borderColor: colors.border },
    row: { flexDirection: "row" as const, alignItems: "center" as const, justifyContent: "space-between" as const },
  }));
  const sorted = [...versions].sort((a, b) => b.versionNo - a.versionNo);
  return (
    <View style={styles.list}>
      {sorted.map((v) => (
        <Pressable key={v.id} onPress={() => onSelect(v)}>
          <Card style={styles.item}>
            <View style={styles.row}>
              <Text style={typography.label}>v{v.versionNo}</Text>
              <StatusBadge status={v.status} />
            </View>
            <Text style={typography.muted}>{new Date(v.createdAt).toLocaleString("ko-KR")}</Text>
          </Card>
        </Pressable>
      ))}
    </View>
  );
}
