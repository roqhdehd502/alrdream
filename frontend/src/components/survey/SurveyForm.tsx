import { useMemo, useState } from "react";
import { StyleSheet, View } from "react-native";
import type { Question, SurveyAnswer, SurveyDefinition } from "../../types";
import { Button } from "../ui/Button";
import { ErrorBanner } from "../ui/Feedback";
import { SingleChoiceField } from "./SingleChoiceField";
import { MultiChoiceField } from "./MultiChoiceField";
import { SurveyTextField } from "./TextField";
import { ScaleField } from "./ScaleField";
import { emptyAnswer, isAnswered } from "./fieldTypes";

function fieldFor(question: Question) {
  switch (question.type) {
    case "SINGLE_CHOICE":
      return SingleChoiceField;
    case "MULTI_CHOICE":
      return MultiChoiceField;
    case "SCALE":
      return ScaleField;
    default:
      return SurveyTextField;
  }
}

export function SurveyForm({
  definition,
  initialAnswers,
  submitting,
  submitLabel = "제출하고 계속하기",
  onSubmit,
}: {
  definition: SurveyDefinition;
  initialAnswers?: SurveyAnswer[];
  submitting?: boolean;
  submitLabel?: string;
  onSubmit: (answers: SurveyAnswer[]) => void;
}) {
  const [answers, setAnswers] = useState<Record<string, SurveyAnswer>>(() => {
    const map: Record<string, SurveyAnswer> = {};
    for (const q of definition.questions) {
      const prev = initialAnswers?.find((a) => a.questionId === q.id);
      map[q.id] = prev ?? emptyAnswer(q);
    }
    return map;
  });
  const [validationError, setValidationError] = useState<string | null>(null);

  const missingCount = useMemo(
    () => definition.questions.filter((q) => q.required && !isAnswered(q, answers[q.id])).length,
    [definition.questions, answers],
  );

  const handleChange = (questionId: string, answer: SurveyAnswer) => {
    setAnswers((prev) => ({ ...prev, [questionId]: answer }));
  };

  const handleSubmit = () => {
    if (missingCount > 0) {
      setValidationError(`아직 답변하지 않은 필수 문항이 ${missingCount}개 있습니다.`);
      return;
    }
    setValidationError(null);
    onSubmit(definition.questions.map((q) => answers[q.id]));
  };

  return (
    <View style={styles.wrap}>
      {definition.questions.map((question) => {
        const Field = fieldFor(question);
        return <Field key={question.id} question={question} answer={answers[question.id]} onChange={(a) => handleChange(question.id, a)} />;
      })}
      <ErrorBanner message={validationError} />
      <Button label={submitLabel} onPress={handleSubmit} loading={submitting} />
    </View>
  );
}

const styles = StyleSheet.create({
  wrap: { gap: 24 },
});
