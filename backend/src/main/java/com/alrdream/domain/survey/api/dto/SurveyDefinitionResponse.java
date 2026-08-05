package com.alrdream.domain.survey.api.dto;

import com.alrdream.domain.survey.domain.SurveyKey;
import com.alrdream.domain.survey.domain.SurveySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record SurveyDefinitionResponse(
		@Schema(description = "설문 정의 ID") UUID id,
		@Schema(description = "설문 종류") SurveyKey surveyKey,
		@Schema(description = "버전") int version,
		@Schema(description = "설문 제목") String title,
		@Schema(description = "문항 목록") List<QuestionDto> questions,
		@Schema(description = "발행 시각") OffsetDateTime createdAt) {

	public static SurveyDefinitionResponse of(
			UUID id, SurveyKey surveyKey, int version, String title, SurveySchema schema, OffsetDateTime createdAt) {
		List<QuestionDto> questions = schema.questions().stream()
				.map(q -> new QuestionDto(
						q.id(),
						q.promptKey(),
						q.type(),
						q.question(),
						q.required(),
						q.options() == null
								? List.of()
								: q.options().stream()
										.map(o -> new QuestionDto.OptionDto(o.key(), o.label()))
										.toList(),
						q.allowUnknown()))
				.toList();
		return new SurveyDefinitionResponse(id, surveyKey, version, title, questions, createdAt);
	}
}
