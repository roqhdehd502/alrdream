import { Pressable, Text, View } from "react-native";
import { useThemedStyles } from "../ui/ThemeContext";
import { fontFamily, radius } from "../ui/theme";
import { QuestionShell } from "./QuestionShell";
import type { FieldProps } from "./fieldTypes";

const SCALE_VALUES = ["1", "2", "3", "4", "5"];

export function ScaleField({ question, answer, onChange }: FieldProps) {
  const selected = answer.values[0];
  const styles = useThemedStyles((colors) => ({
    row: { flexDirection: "row" as const, gap: 10 },
    circle: {
      width: 42,
      height: 42,
      borderRadius: radius.sm,
      borderWidth: 1,
      borderColor: colors.border,
      alignItems: "center" as const,
      justifyContent: "center" as const,
      backgroundColor: colors.surface,
    },
    circleActive: { backgroundColor: colors.primary, borderColor: colors.primary },
    value: { fontSize: 15, fontFamily: fontFamily.semibold, color: colors.text },
    valueActive: { color: "#fff" },
  }));
  return (
    <QuestionShell
      question={question}
      answer={answer}
      onToggleUnknown={(next) => onChange({ ...answer, isUnknown: next, values: next ? [] : answer.values })}
    >
      <View style={styles.row}>
        {SCALE_VALUES.map((value) => {
          const active = value === selected;
          return (
            <Pressable
              key={value}
              style={[styles.circle, active && styles.circleActive]}
              onPress={() => onChange({ ...answer, isUnknown: false, values: [value] })}
            >
              <Text style={[styles.value, active && styles.valueActive]}>{value}</Text>
            </Pressable>
          );
        })}
      </View>
    </QuestionShell>
  );
}
