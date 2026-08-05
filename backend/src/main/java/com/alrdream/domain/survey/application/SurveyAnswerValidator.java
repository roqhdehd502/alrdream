package com.alrdream.domain.survey.application;

import com.alrdream.domain.survey.api.dto.SurveyAnswerDto;
import com.alrdream.domain.survey.domain.SurveySchema;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** [02] §4 문서에서 요구하는 "answers[].type이 설문 정의의 문항 타입과 일치" 크로스체크 + required/allowUnknown 검증. */
@Component
public class SurveyAnswerValidator {

	// LONG_TEXT 기준으로 여유 있게 잡은 상한 — 값 길이에 상한이 전혀 없어 대용량 payload(테스트로 확인: 수십 MB)가
	// 그대로 암호화·저장까지 통과하던 문제(메모리/스토리지 남용 여지)가 있어 추가했다.
	private static final int MAX_VALUE_LENGTH = 10_000;

	public void validate(SurveySchema schema, List<SurveyAnswerDto> answers) {
		Map<String, SurveySchema.Question> byId = schema.questions().stream()
				.collect(Collectors.toMap(SurveySchema.Question::id, q -> q));

		Set<String> answeredIds = new HashSet<>();
		for (SurveyAnswerDto answer : answers) {
			SurveySchema.Question question = byId.get(answer.questionId());
			if (question == null) {
				throw new IllegalArgumentException("정의에 없는 문항입니다: " + answer.questionId());
			}
			if (!answeredIds.add(answer.questionId())) {
				throw new IllegalArgumentException("같은 문항에 답변이 중복됩니다: " + answer.questionId());
			}
			if (question.type() != answer.type()) {
				throw new IllegalArgumentException("문항 타입이 일치하지 않습니다: " + answer.questionId());
			}
			validateValue(question, answer);
		}

		for (SurveySchema.Question question : schema.questions()) {
			if (question.required() && !answeredIds.contains(question.id())) {
				throw new IllegalArgumentException("필수 문항에 응답하지 않았습니다: " + question.id());
			}
		}
	}

	private void validateValue(SurveySchema.Question question, SurveyAnswerDto answer) {
		if (answer.isUnknown()) {
			if (!question.allowUnknown()) {
				throw new IllegalArgumentException("\"모름\" 응답을 허용하지 않는 문항입니다: " + question.id());
			}
			return;
		}

		List<String> values = answer.values();
		if (values == null || values.isEmpty()) {
			throw new IllegalArgumentException("답변 값이 비어 있습니다: " + question.id());
		}
		requireWithinLength(question, values);

		switch (question.type()) {
			case SINGLE_CHOICE -> {
				requireCount(question, values, 1);
				requireValidOptions(question, values);
			}
			case MULTI_CHOICE -> {
				requireNoDuplicates(question, values);
				requireValidOptions(question, values);
			}
			case SHORT_TEXT, LONG_TEXT -> requireCount(question, values, 1);
			case SCALE -> {
				requireCount(question, values, 1);
				requireScaleValue(question, values.get(0));
			}
		}
	}

	private void requireWithinLength(SurveySchema.Question question, List<String> values) {
		for (String value : values) {
			if (value != null && value.length() > MAX_VALUE_LENGTH) {
				throw new IllegalArgumentException(
						"답변 값이 너무 깁니다(" + MAX_VALUE_LENGTH + "자 이하): " + question.id());
			}
		}
	}

	private void requireCount(SurveySchema.Question question, List<String> values, int expected) {
		if (values.size() != expected) {
			throw new IllegalArgumentException("답변 개수가 올바르지 않습니다: " + question.id());
		}
	}

	private void requireNoDuplicates(SurveySchema.Question question, List<String> values) {
		if (new HashSet<>(values).size() != values.size()) {
			throw new IllegalArgumentException("같은 보기를 중복해서 선택했습니다: " + question.id());
		}
	}

	private void requireValidOptions(SurveySchema.Question question, List<String> values) {
		List<SurveySchema.Option> options = question.options();
		if (options == null || options.isEmpty()) {
			// [02] §5-3 DESIGN core_feature_priority처럼 아직 동적 옵션이 없는 경우 — 값 검증을 건너뛴다.
			return;
		}
		Set<String> validKeys = options.stream().map(SurveySchema.Option::key).collect(Collectors.toSet());
		for (String value : values) {
			if (!validKeys.contains(value)) {
				throw new IllegalArgumentException("허용되지 않은 값입니다: " + question.id() + "=" + value);
			}
		}
	}

	private void requireScaleValue(SurveySchema.Question question, String value) {
		try {
			int n = Integer.parseInt(value);
			if (n < 1 || n > 5) {
				throw new IllegalArgumentException("SCALE 값은 1~5여야 합니다: " + question.id());
			}
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("SCALE 값은 숫자여야 합니다: " + question.id());
		}
	}
}
