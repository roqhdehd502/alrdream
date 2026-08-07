import type { Question, SurveyAnswer } from "../../types";

export interface FieldProps {
  question: Question;
  answer: SurveyAnswer;
  onChange: (answer: SurveyAnswer) => void;
}

export function emptyAnswer(question: Question): SurveyAnswer {
  return { questionId: question.id, type: question.type, values: [], isUnknown: false };
}

export function isAnswered(question: Question, answer: SurveyAnswer | undefined): boolean {
  if (!answer) return false;
  if (answer.isUnknown) return question.allowUnknown;
  return answer.values.length > 0 && answer.values.every((v) => v.trim().length > 0);
}
