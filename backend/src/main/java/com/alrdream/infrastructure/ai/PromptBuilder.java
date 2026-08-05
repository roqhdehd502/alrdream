package com.alrdream.infrastructure.ai;

import com.alrdream.domain.survey.api.dto.SurveyAnswerDto;
import com.alrdream.domain.survey.domain.SurveySchema;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * [02] §6 AI 프롬프트 매핑 — 설문 응답을 {@code promptKey} 기준 변수 맵으로 변환한다. 실제 시스템/사용자 프롬프트
 * 구성(섹션 구조 등)은 기획/분석/설계 각 도메인의 책임이고, 여기서는 매핑 규칙만 공통으로 제공한다.
 */
@Component
public class PromptBuilder {

	private static final String UNKNOWN_PLACEHOLDER = "(사용자가 확신하지 못함 — AI가 조사/제안 필요)";

	/**
	 * questionId 기준 답변을 promptKey 기준 맵으로 변환한다. {@code isUnknown=true}인 답변은 값 대신
	 * {@link #UNKNOWN_PLACEHOLDER}로 치환되어, 기획안의 "리스크 & 보완 포인트" 섹션에 반영될 수 있게 한다.
	 */
	public Map<String, String> toPromptVariables(SurveySchema schema, List<SurveyAnswerDto> answers) {
		Map<String, String> promptKeyByQuestionId = schema.questions().stream()
				.collect(Collectors.toMap(SurveySchema.Question::id, SurveySchema.Question::promptKey));

		Map<String, String> variables = new LinkedHashMap<>();
		for (SurveyAnswerDto answer : answers) {
			String promptKey = promptKeyByQuestionId.get(answer.questionId());
			if (promptKey == null) {
				continue;
			}
			String value = answer.isUnknown() ? UNKNOWN_PLACEHOLDER : String.join(", ", answer.values());
			variables.put(promptKey, value);
		}
		return variables;
	}

	/** 변수 맵을 {@code "promptKey: value"} 줄 목록으로 렌더링한다 — 프롬프트 사용자 메시지에 그대로 삽입할 수 있는 형태. */
	public String renderAsText(Map<String, String> variables) {
		StringBuilder text = new StringBuilder();
		variables.forEach((key, value) -> text.append(key).append(": ").append(value).append('\n'));
		return text.toString();
	}
}
