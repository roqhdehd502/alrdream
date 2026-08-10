import { Pressable, Text, View } from "react-native";
import { useThemedStyles } from "../ui/ThemeContext";
import { fontFamily, radius } from "../ui/theme";
import { QuestionShell } from "./QuestionShell";
import type { FieldProps } from "./fieldTypes";

export function SingleChoiceField({ question, answer, onChange }: FieldProps) {
  const selected = answer.values[0];
  const styles = useThemedStyles((colors) => ({
    options: { gap: 8 },
    option: {
      flexDirection: "row" as const,
      alignItems: "center" as const,
      gap: 10,
      borderWidth: 1,
      borderColor: colors.border,
      borderRadius: radius.sm,
      paddingVertical: 10,
      paddingHorizontal: 12,
      backgroundColor: colors.surface,
    },
    optionActive: { borderColor: colors.primary, backgroundColor: colors.primarySoft },
    radio: {
      width: 18,
      height: 18,
      borderRadius: 9,
      borderWidth: 1.5,
      borderColor: colors.border,
      alignItems: "center" as const,
      justifyContent: "center" as const,
    },
    radioActive: { borderColor: colors.primary },
    radioDot: { width: 9, height: 9, borderRadius: 5, backgroundColor: colors.primary },
    optionLabel: { fontSize: 14.5, color: colors.text },
    optionLabelActive: { color: colors.primaryHover, fontFamily: fontFamily.semibold },
  }));
  return (
    <QuestionShell
      question={question}
      answer={answer}
      onToggleUnknown={(next) => onChange({ ...answer, isUnknown: next, values: next ? [] : answer.values })}
    >
      <View style={styles.options}>
        {question.options.map((opt) => {
          const active = opt.key === selected;
          return (
            <Pressable
              key={opt.key}
              style={[styles.option, active && styles.optionActive]}
              onPress={() => onChange({ ...answer, isUnknown: false, values: [opt.key] })}
            >
              <View style={[styles.radio, active && styles.radioActive]}>
                {active ? <View style={styles.radioDot} /> : null}
              </View>
              <Text style={[styles.optionLabel, active && styles.optionLabelActive]}>{opt.label}</Text>
            </Pressable>
          );
        })}
      </View>
    </QuestionShell>
  );
}
