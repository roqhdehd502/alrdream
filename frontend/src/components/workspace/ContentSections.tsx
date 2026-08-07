import { StyleSheet, Text, View } from "react-native";
import { Card } from "../ui/Card";
import { colors, typography } from "../ui/theme";

export function Section({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <Card style={styles.section}>
      <Text style={typography.heading}>{title}</Text>
      <View style={styles.body}>{children}</View>
    </Card>
  );
}

export function TextRow({ label, value }: { label?: string; value: string }) {
  return (
    <View style={styles.row}>
      {label ? <Text style={styles.rowLabel}>{label}</Text> : null}
      <Text style={styles.rowValue}>{value}</Text>
    </View>
  );
}

export function ListRow({ label, items }: { label: string; items: string[] }) {
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

const styles = StyleSheet.create({
  section: { borderColor: colors.border },
  body: { gap: 14 },
  row: { gap: 4 },
  rowLabel: { fontSize: 12.5, fontWeight: "700", color: colors.textMuted, textTransform: "uppercase" },
  rowValue: { fontSize: 14.5, color: colors.text, lineHeight: 21 },
  bullet: { fontSize: 14.5, color: colors.text, lineHeight: 21 },
  stageRow: { flexDirection: "row", gap: 10 },
  stageBadge: {
    width: 24,
    height: 24,
    borderRadius: 12,
    backgroundColor: colors.primarySoft,
    alignItems: "center",
    justifyContent: "center",
  },
  stageBadgeText: { fontSize: 12, fontWeight: "700", color: colors.primaryHover },
  stageTitle: { fontSize: 14.5, fontWeight: "700", color: colors.text },
});
