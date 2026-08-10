import { Pressable, Text, View } from "react-native";
import { useTheme, useThemedStyles } from "../ui/ThemeContext";
import { radius } from "../ui/theme";
import type { Question, SurveyAnswer } from "../../types";

export function QuestionShell({
  question,
  answer,
  onToggleUnknown,
  children,
}: {
  question: Question;
  answer: SurveyAnswer;
  onToggleUnknown: (next: boolean) => void;
  children: React.ReactNode;
}) {
  const { typography } = useTheme();
  const styles = useThemedStyles((colors) => ({
    wrap: { gap: 10 },
    required: { color: colors.danger },
    unknownRow: { flexDirection: "row" as const, alignItems: "center" as const, gap: 8 },
    checkbox: {
      width: 18,
      height: 18,
      borderRadius: 4,
      borderWidth: 1.5,
      borderColor: colors.border,
      backgroundColor: colors.surface,
    },
    checkboxChecked: { backgroundColor: colors.primary, borderColor: colors.primary },
    unknownLabel: { fontSize: 13, color: colors.textMuted },
    unknownNote: {
      fontSize: 13,
      color: colors.textFaint,
      fontStyle: "italic" as const,
      backgroundColor: colors.bg,
      padding: 10,
      borderRadius: radius.sm,
    },
  }));
  return (
    <View style={styles.wrap}>
      <Text style={typography.label}>
        {question.question}
        {question.required && !question.allowUnknown ? <Text style={styles.required}> *</Text> : null}
      </Text>
      {!answer.isUnknown ? children : <Text style={styles.unknownNote}>{'"잘 모르겠어요"로 응답했습니다.'}</Text>}
      {question.allowUnknown ? (
        <Pressable style={styles.unknownRow} onPress={() => onToggleUnknown(!answer.isUnknown)}>
          <View style={[styles.checkbox, answer.isUnknown && styles.checkboxChecked]} />
          <Text style={styles.unknownLabel}>잘 모르겠어요</Text>
        </Pressable>
      ) : null}
    </View>
  );
}
