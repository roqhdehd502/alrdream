package com.alrdream.domain.design.application;

import com.alrdream.domain.ai.application.AiGenerationJobService;
import com.alrdream.domain.ai.domain.AiGenerationJob;
import com.alrdream.domain.ai.domain.AiTargetType;
import com.alrdream.domain.analysis.application.AnalysisVersionService;
import com.alrdream.domain.analysis.domain.AnalysisVersion;
import com.alrdream.domain.analysis.domain.AnalysisVersionStatus;
import com.alrdream.domain.design.domain.DesignVersion;
import com.alrdream.domain.design.domain.DesignVersionRepository;
import com.alrdream.domain.design.domain.DesignVersionStatus;
import com.alrdream.domain.document.api.dto.DocumentResponse;
import com.alrdream.domain.document.application.DocumentService;
import com.alrdream.domain.planning.application.PlanningVersionService;
import com.alrdream.domain.planning.domain.PlanningVersion;
import com.alrdream.domain.survey.api.dto.SurveyAnswerDto;
import com.alrdream.domain.survey.application.SurveyDefinitionService;
import com.alrdream.domain.survey.application.SurveyResponseService;
import com.alrdream.domain.survey.domain.SurveyDefinition;
import com.alrdream.domain.survey.domain.SurveyKey;
import com.alrdream.domain.survey.domain.SurveyResponse;
import com.alrdream.domain.survey.domain.SurveySchema;
import com.alrdream.infrastructure.ai.AiClient;
import com.alrdream.infrastructure.ai.AiGenerationRequest;
import com.alrdream.infrastructure.ai.PromptBuilder;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * [01] 10,11번, [02] §6, [03] §4-2 — 분석 버전 하나 + DESIGN 설문 응답 하나를 입력으로 설계를 생성한다.
 * [02] §6에 따라 프롬프트에는 원래 기획 단계 설문 응답까지 함께 포함해 컨텍스트를 누적한다.
 * "수정"은 새 DESIGN 설문 응답으로 새 버전을 만드는 것 — Planning과 동일한 패턴이라 같은 진입점을 공유한다.
 */
@Service
@Transactional(readOnly = true)
public class DesignVersionService {

	private final DesignVersionRepository designVersionRepository;
	private final AnalysisVersionService analysisVersionService;
	private final PlanningVersionService planningVersionService;
	private final SurveyResponseService surveyResponseService;
	private final SurveyDefinitionService surveyDefinitionService;
	private final AiGenerationJobService aiGenerationJobService;
	private final PromptBuilder promptBuilder;
	private final AiClient aiClient;
	private final DocumentService documentService;

	@PersistenceContext
	private EntityManager entityManager;

	public DesignVersionService(
			DesignVersionRepository designVersionRepository,
			AnalysisVersionService analysisVersionService,
			PlanningVersionService planningVersionService,
			SurveyResponseService surveyResponseService,
			SurveyDefinitionService surveyDefinitionService,
			AiGenerationJobService aiGenerationJobService,
			PromptBuilder promptBuilder,
			AiClient aiClient,
			DocumentService documentService) {
		this.designVersionRepository = designVersionRepository;
		this.analysisVersionService = analysisVersionService;
		this.planningVersionService = planningVersionService;
		this.surveyResponseService = surveyResponseService;
		this.surveyDefinitionService = surveyDefinitionService;
		this.aiGenerationJobService = aiGenerationJobService;
		this.promptBuilder = promptBuilder;
		this.aiClient = aiClient;
		this.documentService = documentService;
	}

	/** [01] 10번(최초 생성)과 11번("수정" — 새 DESIGN 설문 응답으로 새 버전 생성)이 공유하는 단일 진입점. */
	@Transactional
	public AiGenerationJob create(
			UUID workspaceId, UUID planningVersionId, UUID analysisVersionId, UUID userId, UUID surveyResponseId) {
		AnalysisVersion analysisVersion = analysisVersionService.getOwned(analysisVersionId, planningVersionId, workspaceId, userId);
		if (analysisVersion.getStatus() != AnalysisVersionStatus.COMPLETED) {
			throw new IllegalArgumentException("생성이 완료된 분석에서만 설계를 만들 수 있습니다.");
		}

		SurveyResponse designResponse = surveyResponseService.getOwned(surveyResponseId, workspaceId);
		SurveyDefinition designDefinition = surveyDefinitionService.getById(designResponse.getSurveyDefinitionId());
		if (designDefinition.getSurveyKey() != SurveyKey.DESIGN) {
			throw new IllegalArgumentException("설계 생성에는 DESIGN 설문 응답만 사용할 수 있습니다.");
		}

		// [02] §6 — 설계는 이전 단계 컨텍스트를 누적한다. analysis_version_id만으로는 원래 기획 설문 응답을
		// 알 수 없어 planning_version을 한 번 더 거쳐 가져온다.
		PlanningVersion planningVersion = planningVersionService.getOwned(planningVersionId, workspaceId, userId);
		SurveyResponse planningResponse = surveyResponseService.getOwned(planningVersion.getSurveyResponseId(), workspaceId);
		SurveyDefinition planningDefinition = surveyDefinitionService.getById(planningResponse.getSurveyDefinitionId());

		// 동시 생성 요청이 같은 분석 버전의 다음 버전 번호를 동시에 계산해 UNIQUE(analysis_version_id,
		// version_no) 위반이 나는 것을 막는다 (Phase 05/07/08과 동일한 이유).
		entityManager.createNativeQuery("SELECT pg_advisory_xact_lock(hashtext(:key)::bigint)")
				.setParameter("key", "design_version_create:" + analysisVersionId)
				.getSingleResult();

		int nextVersionNo = designVersionRepository.findFirstByAnalysisVersionIdOrderByVersionNoDesc(analysisVersionId)
				.map(v -> v.getVersionNo() + 1)
				.orElse(1);

		DesignVersion designVersion =
				designVersionRepository.save(DesignVersion.create(analysisVersionId, surveyResponseId, nextVersionNo));
		UUID designVersionId = designVersion.getId();

		SurveySchema planningSchema = surveyDefinitionService.parseSchema(planningDefinition);
		List<SurveyAnswerDto> planningAnswers = surveyResponseService.parseAnswers(planningResponse);
		SurveySchema designSchema = surveyDefinitionService.parseSchema(designDefinition);
		List<SurveyAnswerDto> designAnswers = surveyResponseService.parseAnswers(designResponse);
		String analysisContent = analysisVersion.getContent();

		return aiGenerationJobService.submit(
				userId,
				AiTargetType.DESIGN,
				designVersionId,
				() -> generate(designVersionId, planningSchema, planningAnswers, analysisContent, designSchema, designAnswers));
	}

	public List<DesignVersion> list(UUID workspaceId, UUID planningVersionId, UUID analysisVersionId, UUID userId) {
		analysisVersionService.getOwned(analysisVersionId, planningVersionId, workspaceId, userId);
		return designVersionRepository.findAllByAnalysisVersionIdAndDeletedAtIsNullOrderByVersionNoDesc(analysisVersionId);
	}

	public DesignVersion getOwned(
			UUID designVersionId, UUID analysisVersionId, UUID planningVersionId, UUID workspaceId, UUID userId) {
		analysisVersionService.getOwned(analysisVersionId, planningVersionId, workspaceId, userId);
		return designVersionRepository.findByIdAndAnalysisVersionIdAndDeletedAtIsNull(designVersionId, analysisVersionId)
				.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 설계입니다."));
	}

	/** [03] §4-6 — 완료된 설계의 PDF를 조회하거나(이미 생성됨) 새로 생성한다. */
	@Transactional
	public DocumentResponse generatePdf(
			UUID designVersionId, UUID analysisVersionId, UUID planningVersionId, UUID workspaceId, UUID userId) {
		DesignVersion version = getOwned(designVersionId, analysisVersionId, planningVersionId, workspaceId, userId);
		if (version.getStatus() != DesignVersionStatus.COMPLETED) {
			throw new IllegalArgumentException("생성이 완료된 설계만 PDF로 내려받을 수 있습니다.");
		}
		return documentService.getOrGenerate(
				AiTargetType.DESIGN, version.getId(), "pdf/design", version.getContent(),
				Map.of("versionNo", version.getVersionNo()));
	}

	/** [01] 11번 문맥에 준하는 다중 삭제 — 기획/분석과 동일하게 다중 선택 소프트 삭제를 지원한다. */
	@Transactional
	public void deleteAll(
			UUID workspaceId, UUID planningVersionId, UUID analysisVersionId, UUID userId, List<UUID> designVersionIds) {
		analysisVersionService.getOwned(analysisVersionId, planningVersionId, workspaceId, userId);
		List<DesignVersion> versions =
				designVersionRepository.findAllByIdInAndAnalysisVersionIdAndDeletedAtIsNull(designVersionIds, analysisVersionId);
		if (versions.size() != designVersionIds.size()) {
			throw new IllegalArgumentException("존재하지 않는 설계가 포함되어 있습니다.");
		}
		versions.forEach(DesignVersion::markDeleted);
	}

	// AiGenerationJobService가 커밋 이후 별도 Virtual Thread에서 실행하므로 트랜잭션/프록시 컨텍스트가 없다 —
	// Planning/Analysis와 동일한 이유로 리포지토리 호출 자체의 트랜잭션 경계에 완료/실패 반영을 맡긴다.
	private void generate(
			UUID designVersionId,
			SurveySchema planningSchema,
			List<SurveyAnswerDto> planningAnswers,
			String analysisContent,
			SurveySchema designSchema,
			List<SurveyAnswerDto> designAnswers) {
		try {
			String planningPromptText = promptBuilder.renderAsText(promptBuilder.toPromptVariables(planningSchema, planningAnswers));
			String designPromptText = promptBuilder.renderAsText(promptBuilder.toPromptVariables(designSchema, designAnswers));
			String userPrompt = "[기획 단계 설문 응답]\n" + planningPromptText
					+ "\n[분석 결과]\n" + analysisContent
					+ "\n\n[설계 설문 응답]\n" + designPromptText;

			AiGenerationRequest request = new AiGenerationRequest(
					DesignGenerationSpec.SYSTEM_PROMPT,
					userPrompt,
					DesignGenerationSpec.TOOL_NAME,
					DesignGenerationSpec.TOOL_DESCRIPTION,
					DesignGenerationSpec.SCHEMA_JSON);
			String content = aiClient.generateStructuredJson(request);
			completeVersion(designVersionId, content);
		} catch (RuntimeException e) {
			failVersion(designVersionId);
			throw e;
		}
	}

	private void completeVersion(UUID id, String content) {
		designVersionRepository.findById(id).ifPresent(version -> {
			version.complete(content);
			designVersionRepository.save(version);
		});
	}

	private void failVersion(UUID id) {
		designVersionRepository.findById(id).ifPresent(version -> {
			version.fail();
			designVersionRepository.save(version);
		});
	}
}
