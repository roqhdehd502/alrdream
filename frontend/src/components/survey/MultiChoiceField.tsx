import { Pressable, Text, View } from "react-native";
import { useThemedStyles } from "../ui/ThemeContext";
import { fontFamily, radius } from "../ui/theme";
import { QuestionShell } from "./QuestionShell";
import type { FieldProps } from "./fieldTypes";

export function MultiChoiceField({ question, answer, onChange }: FieldProps) {
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
    checkbox: { width: 18, height: 18, borderRadius: 4, borderWidth: 1.5, borderColor: colors.border },
    checkboxActive: { backgroundColor: colors.primary, borderColor: colors.primary },
    optionLabel: { fontSize: 14.5, color: colors.text },
    optionLabelActive: { color: colors.primaryHover, fontFamily: fontFamily.semibold },
  }));

  const toggle = (key: string) => {
    const has = answer.values.includes(key);
    const values = has ? answer.values.filter((v) => v !== key) : [...answer.values, key];
    onChange({ ...answer, isUnknown: false, values });
  };

  return (
    <QuestionShell
      question={question}
      answer={answer}
      onToggleUnknown={(next) => onChange({ ...answer, isUnknown: next, values: next ? [] : answer.values })}
    >
      <View style={styles.options}>
        {question.options.map((opt) => {
          const active = answer.values.includes(opt.key);
          return (
            <Pressable
              key={opt.key}
              style={[styles.option, active && styles.optionActive]}
              onPress={() => toggle(opt.key)}
            >
              <View style={[styles.checkbox, active && styles.checkboxActive]} />
              <Text style={[styles.optionLabel, active && styles.optionLabelActive]}>{opt.label}</Text>
            </Pressable>
          );
        })}
      </View>
    </QuestionShell>
  );
}
