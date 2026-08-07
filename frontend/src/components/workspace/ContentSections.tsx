import { Text, View } from "react-native";
import { Card } from "../ui/Card";
import { useTheme, useThemedStyles } from "../ui/ThemeContext";
import { fontFamily } from "../ui/theme";

export function Section({ title, children }: { title: string; children: React.ReactNode }) {
  const { typography } = useTheme();
  const styles = useThemedStyles((colors) => ({
    section: { borderColor: colors.border },
    body: { gap: 14 },
  }));
  return (
    <Card style={styles.section}>
      <Text style={typography.heading}>{title}</Text>
      <View style={styles.body}>{children}</View>
    </Card>
  );
}

export function TextRow({ label, value }: { label?: string; value: string }) {
  const styles = useThemedStyles((colors) => ({
    row: { gap: 4 },
    rowLabel: { fontSize: 12.5, fontFamily: fontFamily.bold, color: colors.textMuted, textTransform: "uppercase" as const },
    rowValue: { fontSize: 14.5, fontFamily: fontFamily.regular, color: colors.text, lineHeight: 21 },
  }));
  return (
    <View style={styles.row}>
      {label ? <Text style={styles.rowLabel}>{label}</Text> : null}
      <Text style={styles.rowValue}>{value}</Text>
    </View>
  );
}

export function ListRow({ label, items }: { label: string; items: string[] }) {
  const styles = useThemedStyles((colors) => ({
    row: { gap: 4 },
    rowLabel: { fontSize: 12.5, fontFamily: fontFamily.bold, color: colors.textMuted, textTransform: "uppercase" as const },
    bullet: { fontSize: 14.5, fontFamily: fontFamily.regular, color: colors.text, lineHeight: 21 },
  }));
  return (
    <View style={styles.row}>
      <Text style={styles.rowLabel}>{label}</Text>
      {items.map((item, i) => (
        <Text key={i} style={styles.bullet}>
          • {item}
        </Text>
      ))}
    </View>
  );
}

export function StageList({ items }: { items: { stage?: string; phase?: string; tasks?: string; actions?: string }[] }) {
  const { typography } = useTheme();
  const styles = useThemedStyles((colors) => ({
    stageRow: { flexDirection: "row" as const, gap: 10 },
    stageBadge: {
      width: 24,
      height: 24,
      borderRadius: 12,
      backgroundColor: colors.primarySoft,
      alignItems: "center" as const,
      justifyContent: "center" as const,
    },
    stageBadgeText: { fontSize: 12, fontFamily: fontFamily.bold, color: colors.primaryHover },
    stageTitle: { fontSize: 14.5, fontFamily: fontFamily.bold, color: colors.text },
  }));
  return (
    <View style={{ gap: 10 }}>
      {items.map((item, i) => (
        <View key={i} style={styles.stageRow}>
          <View style={styles.stageBadge}>
            <Text style={styles.stageBadgeText}>{i + 1}</Text>
          </View>
          <View style={{ flex: 1, gap: 2 }}>
            <Text style={styles.stageTitle}>{item.stage ?? item.phase}</Text>
            <Text style={typography.body}>{item.actions ?? item.tasks}</Text>
          </View>
        </View>
      ))}
    </View>
  );
}
