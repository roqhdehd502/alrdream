import { Text, View } from "react-native";
import { Card } from "../ui/Card";
import { useTheme, useThemedStyles } from "../ui/ThemeContext";
import { diffContent, prettyPath } from "./contentDiff";

export function VersionDiffView({
  beforeLabel,
  afterLabel,
  before,
  after,
}: {
  beforeLabel: string;
  afterLabel: string;
  before: unknown;
  after: unknown;
}) {
  const { typography } = useTheme();
  const styles = useThemedStyles((colors) => ({
    wrap: { gap: 10 },
    row: { gap: 6, borderColor: colors.border },
    pathLabel: { color: colors.textMuted },
    before: { color: colors.danger, textDecorationLine: "line-through" as const },
    after: { color: colors.success },
  }));
  const rows = diffContent(before, after);

  if (rows.length === 0) {
    return <Text style={typography.muted}>두 버전 사이에 달라진 내용이 없습니다.</Text>;
  }

  return (
    <View style={styles.wrap}>
      <Text style={typography.muted}>
        {beforeLabel} → {afterLabel} 사이 {rows.length}개 항목이 달라졌습니다.
      </Text>
      {rows.map((row) => (
        <Card key={row.path} style={styles.row}>
          <Text style={[typography.label, styles.pathLabel]}>{prettyPath(row.path)}</Text>
          {row.before !== null && <Text style={styles.before}>{row.before || "(비어있음)"}</Text>}
          {row.after !== null && <Text style={styles.after}>{row.after || "(비어있음)"}</Text>}
        </Card>
      ))}
    </View>
  );
}
