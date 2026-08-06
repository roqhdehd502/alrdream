package com.alrdream.domain.prompt.application;

import com.alrdream.domain.ai.domain.AiTargetType;
import com.alrdream.domain.prompt.api.dto.PublishPromptTemplateRequest;
import com.alrdream.domain.prompt.domain.PromptTemplate;
import com.alrdream.domain.prompt.domain.PromptTemplateRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** [03] §2-1 Admin의 AI 프롬프트 템플릿 관리. {@link com.alrdream.domain.survey.application.SurveyDefinitionService}와 동일한 발행 패턴. */
@Service
@Transactional(readOnly = true)
public class PromptTemplateService {

	private final PromptTemplateRepository promptTemplateRepository;

	@PersistenceContext
	private EntityManager entityManager;

	public PromptTemplateService(PromptTemplateRepository promptTemplateRepository) {
		this.promptTemplateRepository = promptTemplateRepository;
	}

	@Transactional
	public PromptTemplate publish(PublishPromptTemplateRequest request) {
		// survey_definitions 발행과 동일한 동시성 문제(같은 promptType을 향한 동시 발행이 "최신 버전 + 1" 계산에서
		// 경합해 UNIQUE(prompt_type, version) 위반이 나는 것)를 advisory lock으로 막는다.
		entityManager.createNativeQuery("SELECT pg_advisory_xact_lock(hashtext(:key)::bigint)")
				.setParameter("key", "prompt_template_publish:" + request.promptType().name())
				.getSingleResult();

		int nextVersion = promptTemplateRepository.findFirstByPromptTypeOrderByVersionDesc(request.promptType())
				.map(t -> t.getVersion() + 1)
				.orElse(1);
		return promptTemplateRepository.save(PromptTemplate.create(
				request.promptType(), nextVersion, request.toolName(), request.toolDescription(),
				request.systemPrompt(), request.schemaJson()));
	}

	public List<PromptTemplate> list(AiTargetType promptType) {
		return promptType == null
				? promptTemplateRepository.findAllByOrderByPromptTypeAscVersionDesc()
				: promptTemplateRepository.findAllByPromptTypeOrderByVersionDesc(promptType);
	}

	public PromptTemplate getById(UUID id) {
		return promptTemplateRepository.findById(id)
				.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 프롬프트 템플릿입니다."));
	}

	/** 기획/분석/설계 생성 시 실제로 사용할, 해당 타입의 최신(=현재 활성) 버전. */
	public PromptTemplate getActive(AiTargetType promptType) {
		return promptTemplateRepository.findFirstByPromptTypeOrderByVersionDesc(promptType)
				.orElseThrow(() -> new IllegalStateException("발행된 프롬프트 템플릿이 없습니다: " + promptType));
	}
}
