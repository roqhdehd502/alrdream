package com.alrdream.domain.survey.domain;

import java.util.List;

/** [02] §3 설문 정의 스키마. {@code survey_definitions.schema}(jsonb)에 이 구조 그대로 직렬화되어 저장된다. */
public record SurveySchema(String surveyKey, int version, String title, List<Question> questions) {

	public record Question(
			String id,
			String promptKey,
			QuestionType type,
			String question,
			boolean required,
			List<Option> options,
			boolean allowUnknown) {
	}

	public record Option(String key, String label) {
	}
}
