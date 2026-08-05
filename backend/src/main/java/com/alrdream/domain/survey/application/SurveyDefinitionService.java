package com.alrdream.domain.survey.application;

import com.alrdream.domain.survey.api.dto.PublishSurveyDefinitionRequest;
import com.alrdream.domain.survey.api.dto.QuestionDto;
import com.alrdream.domain.survey.domain.DesignFeatureOptionResolver;
import com.alrdream.domain.survey.domain.SurveyDefinition;
import com.alrdream.domain.survey.domain.SurveyDefinitionRepository;
import com.alrdream.domain.survey.domain.SurveyKey;
import com.alrdream.domain.survey.domain.SurveySchema;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class SurveyDefinitionService {

	// Spring Boot 4.x가 자동 구성하는 ObjectMapper 빈은 Jackson 3.x 타입이라 여기서 필요한 Jackson 2.x API와
	// 맞지 않는다(SecurityConfig와 동일한 이유) — 빈을 주입받는 대신 독립적으로 생성한다.
	private final ObjectMapper objectMapper = new ObjectMapper();

	private final SurveyDefinitionRepository surveyDefinitionRepository;
	private final DesignFeatureOptionResolver designFeatureOptionResolver;

	@PersistenceContext
	private EntityManager entityManager;

	public SurveyDefinitionService(
			SurveyDefinitionRepository surveyDefinitionRepository,
			DesignFeatureOptionResolver designFeatureOptionResolver) {
		this.surveyDefinitionRepository = surveyDefinitionRepository;
		this.designFeatureOptionResolver = designFeatureOptionResolver;
	}

	@Transactional
	public SurveyDefinition publish(PublishSurveyDefinitionRequest request) {
		// 같은 surveyKey를 향한 동시 발행 요청이 "최신 버전 + 1" 계산에서 경합하면 UNIQUE(survey_key, version)
		// 제약 위반으로 500이 나던 문제(실제 동시 발행 테스트로 재현 확인)가 있었다 — 트랜잭션 advisory lock으로
		// 같은 surveyKey에 대한 발행을 직렬화해서 막는다. 트랜잭션 종료 시 자동 해제되고, 서로 다른 surveyKey끼리는
		// 블로킹하지 않는다.
		entityManager.createNativeQuery("SELECT pg_advisory_xact_lock(hashtext(:key)::bigint)")
				.setParameter("key", "survey_definition_publish:" + request.surveyKey().name())
				.getSingleResult();

		int nextVersion = surveyDefinitionRepository.findFirstBySurveyKeyOrderByVersionDesc(request.surveyKey())
				.map(d -> d.getVersion() + 1)
				.orElse(1);
		SurveySchema schema = new SurveySchema(
				request.surveyKey().name(),
				nextVersion,
				request.title(),
				request.questions().stream().map(this::toSchemaQuestion).toList());
		return surveyDefinitionRepository.save(
				SurveyDefinition.create(request.surveyKey(), nextVersion, request.title(), toJson(schema)));
	}

	public List<SurveyDefinition> list(SurveyKey surveyKey) {
		return surveyKey == null
				? surveyDefinitionRepository.findAllByOrderBySurveyKeyAscVersionDesc()
				: surveyDefinitionRepository.findAllBySurveyKeyOrderByVersionDesc(surveyKey);
	}

	public SurveyDefinition getById(UUID id) {
		return surveyDefinitionRepository.findById(id)
				.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 설문 정의입니다."));
	}

	public SurveyDefinition getLatestDefinition(SurveyKey surveyKey) {
		return surveyDefinitionRepository.findFirstBySurveyKeyOrderByVersionDesc(surveyKey)
				.orElseThrow(() -> new IllegalArgumentException("발행된 설문이 없습니다: " + surveyKey));
	}

	public SurveySchema parseSchema(SurveyDefinition definition) {
		try {
			return objectMapper.readValue(definition.getSchema(), SurveySchema.class);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("설문 정의 스키마 파싱에 실패했습니다.", e);
		}
	}

	/** [02] §5-3 DESIGN 설문은 core_feature_priority 옵션을 워크스페이스별로 동적 주입한다. */
	public SurveySchema getResolvedSchema(SurveyKey surveyKey, UUID workspaceId) {
		SurveySchema schema = parseSchema(getLatestDefinition(surveyKey));
		if (surveyKey != SurveyKey.DESIGN) {
			return schema;
		}
		List<SurveySchema.Option> dynamicOptions = designFeatureOptionResolver.resolve(workspaceId);
		if (dynamicOptions.isEmpty()) {
			return schema;
		}
		List<SurveySchema.Question> questions = schema.questions().stream()
				.map(q -> "core_feature_priority".equals(q.promptKey())
						? new SurveySchema.Question(
								q.id(), q.promptKey(), q.type(), q.question(), q.required(), dynamicOptions, q.allowUnknown())
						: q)
				.toList();
		return new SurveySchema(schema.surveyKey(), schema.version(), schema.title(), questions);
	}

	private SurveySchema.Question toSchemaQuestion(QuestionDto dto) {
		List<SurveySchema.Option> options = dto.options() == null
				? List.of()
				: dto.options().stream().map(o -> new SurveySchema.Option(o.key(), o.label())).toList();
		return new SurveySchema.Question(dto.id(), dto.promptKey(), dto.type(), dto.question(), dto.required(), options, dto.allowUnknown());
	}

	private String toJson(SurveySchema schema) {
		try {
			return objectMapper.writeValueAsString(schema);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("설문 정의 스키마 직렬화에 실패했습니다.", e);
		}
	}
}
