package com.alrdream.domain.survey.application;

import com.alrdream.domain.survey.api.dto.SubmitSurveyResponseRequest;
import com.alrdream.domain.survey.api.dto.SurveyAnswerDto;
import com.alrdream.domain.survey.domain.SurveyDefinition;
import com.alrdream.domain.survey.domain.SurveyResponse;
import com.alrdream.domain.survey.domain.SurveyResponseRepository;
import com.alrdream.domain.survey.domain.SurveySchema;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class SurveyResponseService {

	// SurveyDefinitionService와 동일한 이유(Jackson 2.x/3.x 혼재)로 독립 생성.
	private final ObjectMapper objectMapper = new ObjectMapper();

	private final SurveyResponseRepository surveyResponseRepository;
	private final SurveyDefinitionService surveyDefinitionService;
	private final SurveyAnswerValidator surveyAnswerValidator;

	public SurveyResponseService(
			SurveyResponseRepository surveyResponseRepository,
			SurveyDefinitionService surveyDefinitionService,
			SurveyAnswerValidator surveyAnswerValidator) {
		this.surveyResponseRepository = surveyResponseRepository;
		this.surveyDefinitionService = surveyDefinitionService;
		this.surveyAnswerValidator = surveyAnswerValidator;
	}

	@Transactional
	public SurveyResponse submit(UUID workspaceId, SubmitSurveyResponseRequest request) {
		SurveyDefinition definition = surveyDefinitionService.getLatestDefinition(request.surveyKey());
		SurveySchema resolvedSchema = surveyDefinitionService.getResolvedSchema(request.surveyKey(), workspaceId);
		surveyAnswerValidator.validate(resolvedSchema, request.answers());
		return surveyResponseRepository.save(
				SurveyResponse.create(definition.getId(), workspaceId, toJson(request.answers())));
	}

	public List<SurveyResponse> list(UUID workspaceId) {
		return surveyResponseRepository.findAllByWorkspaceIdOrderBySubmittedAtDesc(workspaceId);
	}

	public SurveyResponse getOwned(UUID responseId, UUID workspaceId) {
		return surveyResponseRepository.findByIdAndWorkspaceId(responseId, workspaceId)
				.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 설문 응답입니다."));
	}

	public List<SurveyAnswerDto> parseAnswers(SurveyResponse response) {
		try {
			return objectMapper.readValue(response.getAnswers(), new TypeReference<List<SurveyAnswerDto>>() {
			});
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("설문 응답 파싱에 실패했습니다.", e);
		}
	}

	private String toJson(List<SurveyAnswerDto> answers) {
		try {
			return objectMapper.writeValueAsString(answers);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("설문 응답 직렬화에 실패했습니다.", e);
		}
	}
}
