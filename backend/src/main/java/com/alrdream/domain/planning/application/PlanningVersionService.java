package com.alrdream.domain.planning.application;

import com.alrdream.domain.ai.application.AiGenerationJobService;
import com.alrdream.domain.ai.domain.AiGenerationJob;
import com.alrdream.domain.ai.domain.AiTargetType;
import com.alrdream.domain.document.api.dto.DocumentResponse;
import com.alrdream.domain.document.application.DocumentService;
import com.alrdream.domain.planning.domain.PlanningVersion;
import com.alrdream.domain.planning.domain.PlanningVersionRepository;
import com.alrdream.domain.planning.domain.PlanningVersionStatus;
import com.alrdream.domain.survey.api.dto.SurveyAnswerDto;
import com.alrdream.domain.survey.application.SurveyDefinitionService;
import com.alrdream.domain.survey.application.SurveyResponseService;
import com.alrdream.domain.survey.domain.SurveyDefinition;
import com.alrdream.domain.survey.domain.SurveyKey;
import com.alrdream.domain.survey.domain.SurveyResponse;
import com.alrdream.domain.survey.domain.SurveySchema;
import com.alrdream.domain.workspace.application.WorkspaceService;
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

/** [01] 2,5,6번, [03] §4-3/§4-4 — 기획안 버전 생성/조회/삭제. "수정"은 새 설문 응답으로 새 버전을 만드는 것([02] §7). */
@Service
@Transactional(readOnly = true)
public class PlanningVersionService {

	private final PlanningVersionRepository planningVersionRepository;
	private final WorkspaceService workspaceService;
	private final SurveyResponseService surveyResponseService;
	private final SurveyDefinitionService surveyDefinitionService;
	private final AiGenerationJobService aiGenerationJobService;
	private final PromptBuilder promptBuilder;
	private final AiClient aiClient;
	private final DocumentService documentService;

	@PersistenceContext
	private EntityManager entityManager;

	public PlanningVersionService(
			PlanningVersionRepository planningVersionRepository,
			WorkspaceService workspaceService,
			SurveyResponseService surveyResponseService,
			SurveyDefinitionService surveyDefinitionService,
			AiGenerationJobService aiGenerationJobService,
			PromptBuilder promptBuilder,
			AiClient aiClient,
			DocumentService documentService) {
		this.planningVersionRepository = planningVersionRepository;
		this.workspaceService = workspaceService;
		this.surveyResponseService = surveyResponseService;
		this.surveyDefinitionService = surveyDefinitionService;
		this.aiGenerationJobService = aiGenerationJobService;
		this.promptBuilder = promptBuilder;
		this.aiClient = aiClient;
		this.documentService = documentService;
	}

	/**
	 * [01] 2번(최초 생성)과 5번("수정" — 새 설문 응답으로 새 버전 생성)이 공유하는 단일 진입점.
	 * 생성된 기획안은 아직 GENERATING 상태라, 클라이언트가 진행 상황을 폴링할 수 있도록 Job 정보를 반환한다.
	 */
	@Transactional
	public AiGenerationJob create(UUID workspaceId, UUID userId, UUID surveyResponseId) {
		workspaceService.getOwned(workspaceId, userId);
		SurveyResponse response = surveyResponseService.getOwned(surveyResponseId, workspaceId);
		SurveyDefinition definition = surveyDefinitionService.getById(response.getSurveyDefinitionId());
		if (definition.getSurveyKey() == SurveyKey.DESIGN) {
			throw new IllegalArgumentException("기획 생성에는 PLANNING 설문 응답만 사용할 수 있습니다.");
		}

		// 동시 생성 요청이 같은 워크스페이스의 다음 버전 번호를 동시에 계산해 UNIQUE(workspace_id, version_no)
		// 위반이 나는 것을 막는다 (survey_definitions 발행 동시성 문제와 동일한 이유 — Phase 05 참고).
		entityManager.createNativeQuery("SELECT pg_advisory_xact_lock(hashtext(:key)::bigint)")
				.setParameter("key", "planning_version_create:" + workspaceId)
				.getSingleResult();

		int nextVersionNo = planningVersionRepository.findFirstByWorkspaceIdOrderByVersionNoDesc(workspaceId)
				.map(v -> v.getVersionNo() + 1)
				.orElse(1);

		PlanningVersion planningVersion = planningVersionRepository.save(
				PlanningVersion.create(workspaceId, surveyResponseId, nextVersionNo));
		UUID planningVersionId = planningVersion.getId();

		SurveySchema schema = surveyDefinitionService.parseSchema(definition);
		List<SurveyAnswerDto> answers = surveyResponseService.parseAnswers(response);

		return aiGenerationJobService.submit(
				userId, AiTargetType.PLANNING, planningVersionId, () -> generate(planningVersionId, schema, answers));
	}

	public List<PlanningVersion> list(UUID workspaceId, UUID userId) {
		workspaceService.getOwned(workspaceId, userId);
		return planningVersionRepository.findAllByWorkspaceIdAndDeletedAtIsNullOrderByVersionNoDesc(workspaceId);
	}

	public PlanningVersion getOwned(UUID versionId, UUID workspaceId, UUID userId) {
		workspaceService.getOwned(workspaceId, userId);
		return planningVersionRepository.findByIdAndWorkspaceIdAndDeletedAtIsNull(versionId, workspaceId)
				.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 기획안입니다."));
	}

	/** [03] §4-6 — 완료된 기획안의 PDF를 조회하거나(이미 생성됨) 새로 생성한다. */
	@Transactional
	public DocumentResponse generatePdf(UUID versionId, UUID workspaceId, UUID userId) {
		PlanningVersion version = getOwned(versionId, workspaceId, userId);
		if (version.getStatus() != PlanningVersionStatus.COMPLETED) {
			throw new IllegalArgumentException("생성이 완료된 기획안만 PDF로 내려받을 수 있습니다.");
		}
		return documentService.getOrGenerate(
				AiTargetType.PLANNING, version.getId(), "pdf/planning", version.getContent(),
				Map.of("versionNo", version.getVersionNo()));
	}

	/** [01] 6번 — 다중 선택 소프트 삭제. */
	@Transactional
	public void deleteAll(UUID workspaceId, UUID userId, List<UUID> versionIds) {
		workspaceService.getOwned(workspaceId, userId);
		List<PlanningVersion> versions =
				planningVersionRepository.findAllByIdInAndWorkspaceIdAndDeletedAtIsNull(versionIds, workspaceId);
		if (versions.size() != versionIds.size()) {
			throw new IllegalArgumentException("존재하지 않는 기획안이 포함되어 있습니다.");
		}
		versions.forEach(PlanningVersion::markDeleted);
	}

	// AiGenerationJobService가 커밋 이후 별도 Virtual Thread에서 실행하므로 트랜잭션 컨텍스트가 없다 — 이 안에서
	// this.xxx()로 @Transactional 메서드를 호출해도 Spring 프록시를 거치지 않아 적용되지 않는다(자가 호출 문제).
	// 그래서 완료/실패 반영은 리포지토리 호출 자체의 트랜잭션 경계에 맡긴다(AiGenerationJobService.updateStatus와 동일 패턴).
	private void generate(UUID planningVersionId, SurveySchema schema, List<SurveyAnswerDto> answers) {
		try {
			Map<String, String> variables = promptBuilder.toPromptVariables(schema, answers);
			String userPrompt = promptBuilder.renderAsText(variables);
			AiGenerationRequest request = new AiGenerationRequest(
					PlanningGenerationSpec.SYSTEM_PROMPT,
					userPrompt,
					PlanningGenerationSpec.TOOL_NAME,
					PlanningGenerationSpec.TOOL_DESCRIPTION,
					PlanningGenerationSpec.SCHEMA_JSON);
			String content = aiClient.generateStructuredJson(request);
			completeVersion(planningVersionId, content);
		} catch (RuntimeException e) {
			failVersion(planningVersionId);
			throw e;
		}
	}

	private void completeVersion(UUID id, String content) {
		planningVersionRepository.findById(id).ifPresent(version -> {
			version.complete(content);
			planningVersionRepository.save(version);
		});
	}

	private void failVersion(UUID id) {
		planningVersionRepository.findById(id).ifPresent(version -> {
			version.fail();
			planningVersionRepository.save(version);
		});
	}
}
