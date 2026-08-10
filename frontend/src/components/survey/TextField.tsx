import { TextInput } from "react-native";
import { useTheme, useThemedStyles } from "../ui/ThemeContext";
import { radius } from "../ui/theme";
import { QuestionShell } from "./QuestionShell";
import type { FieldProps } from "./fieldTypes";

export function SurveyTextField({ question, answer, onChange }: FieldProps) {
  const { colors } = useTheme();
  const styles = useThemedStyles((colors) => ({
    input: {
      borderWidth: 1,
      borderColor: colors.border,
      borderRadius: radius.sm,
      paddingVertical: 10,
      paddingHorizontal: 12,
      fontSize: 14.5,
      color: colors.text,
      backgroundColor: colors.surface,
    },
    multiline: { minHeight: 96, textAlignVertical: "top" as const },
  }));
  const multiline = question.type === "LONG_TEXT";
  return (
    <QuestionShell
      question={question}
      answer={answer}
      onToggleUnknown={(next) => onChange({ ...answer, isUnknown: next, values: next ? [] : answer.values })}
    >
      <TextInput
        style={[styles.input, multiline && styles.multiline]}
        value={answer.values[0] ?? ""}
        onChangeText={(text) => onChange({ ...answer, isUnknown: false, values: [text] })}
        multiline={multiline}
        numberOfLines={multiline ? 4 : 1}
        placeholder="답변을 입력해주세요"
        placeholderTextColor={colors.textFaint}
      />
    </QuestionShell>
  );
}
