package com.alrdream.domain.analysis.application;

import com.alrdream.domain.ai.application.AiGenerationJobService;
import com.alrdream.domain.ai.domain.AiGenerationJob;
import com.alrdream.domain.ai.domain.AiTargetType;
import com.alrdream.domain.analysis.domain.AnalysisVersion;
import com.alrdream.domain.analysis.domain.AnalysisVersionRepository;
import com.alrdream.domain.analysis.domain.AnalysisVersionStatus;
import com.alrdream.domain.document.api.dto.DocumentResponse;
import com.alrdream.domain.document.application.DocumentService;
import com.alrdream.domain.planning.application.PlanningVersionService;
import com.alrdream.domain.planning.domain.PlanningVersion;
import com.alrdream.domain.planning.domain.PlanningVersionStatus;
import com.alrdream.domain.prompt.application.PromptTemplateService;
import com.alrdream.domain.prompt.domain.PromptTemplate;
import com.alrdream.infrastructure.ai.AiClient;
import com.alrdream.infrastructure.ai.AiGenerationRequest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * [01] 7,8,9번, [03] §4-2 — 설문 없이 기획안 버전의 본문을 입력으로 분석을 생성한다. 입력이 항상 같은 기획안
 * 본문뿐이라 "생성"과 "수정"에 차이가 없다 — 같은 진입점을 다시 호출하면 새 버전이 만들어진다.
 */
@Service
@Transactional(readOnly = true)
public class AnalysisVersionService {

	private final AnalysisVersionRepository analysisVersionRepository;
	private final PlanningVersionService planningVersionService;
	private final AiGenerationJobService aiGenerationJobService;
	private final PromptTemplateService promptTemplateService;
	private final AiClient aiClient;
	private final DocumentService documentService;

	@PersistenceContext
	private EntityManager entityManager;

	public AnalysisVersionService(
			AnalysisVersionRepository analysisVersionRepository,
			PlanningVersionService planningVersionService,
			AiGenerationJobService aiGenerationJobService,
			PromptTemplateService promptTemplateService,
			AiClient aiClient,
			DocumentService documentService) {
		this.analysisVersionRepository = analysisVersionRepository;
		this.planningVersionService = planningVersionService;
		this.aiGenerationJobService = aiGenerationJobService;
		this.promptTemplateService = promptTemplateService;
		this.aiClient = aiClient;
		this.documentService = documentService;
	}

	/** [01] 7번(최초 생성)과 8번("수정" — 재생성)이 공유하는 단일 진입점. */
	@Transactional
	public AiGenerationJob create(UUID workspaceId, UUID planningVersionId, UUID userId) {
		PlanningVersion planningVersion = planningVersionService.getOwned(planningVersionId, workspaceId, userId);
		if (planningVersion.getStatus() != PlanningVersionStatus.COMPLETED) {
			throw new IllegalArgumentException("생성이 완료된 기획안에서만 분석을 만들 수 있습니다.");
		}

		// 동시 생성 요청이 같은 기획안 버전의 다음 버전 번호를 동시에 계산해 UNIQUE(planning_version_id,
		// version_no) 위반이 나는 것을 막는다 (Phase 05/07과 동일한 이유).
		entityManager.createNativeQuery("SELECT pg_advisory_xact_lock(hashtext(:key)::bigint)")
				.setParameter("key", "analysis_version_create:" + planningVersionId)
				.getSingleResult();

		int nextVersionNo = analysisVersionRepository.findFirstByPlanningVersionIdOrderByVersionNoDesc(planningVersionId)
				.map(v -> v.getVersionNo() + 1)
				.orElse(1);

		AnalysisVersion analysisVersion =
				analysisVersionRepository.save(AnalysisVersion.create(planningVersionId, nextVersionNo));
		UUID analysisVersionId = analysisVersion.getId();
		String planningContent = planningVersion.getContent();

		return aiGenerationJobService.submit(
				userId, AiTargetType.ANALYSIS, analysisVersionId, () -> generate(analysisVersionId, planningContent));
	}

	public List<AnalysisVersion> list(UUID workspaceId, UUID planningVersionId, UUID userId) {
		planningVersionService.getOwned(planningVersionId, workspaceId, userId);
		return analysisVersionRepository.findAllByPlanningVersionIdAndDeletedAtIsNullOrderByVersionNoDesc(planningVersionId);
	}

	public AnalysisVersion getOwned(UUID analysisVersionId, UUID planningVersionId, UUID workspaceId, UUID userId) {
		planningVersionService.getOwned(planningVersionId, workspaceId, userId);
		return analysisVersionRepository
				.findByIdAndPlanningVersionIdAndDeletedAtIsNull(analysisVersionId, planningVersionId)
				.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 분석입니다."));
	}

	/** [03] §4-6 — 완료된 분석의 PDF를 조회하거나(이미 생성됨) 새로 생성한다. */
	@Transactional
	public DocumentResponse generatePdf(UUID analysisVersionId, UUID planningVersionId, UUID workspaceId, UUID userId) {
		AnalysisVersion version = getOwned(analysisVersionId, planningVersionId, workspaceId, userId);
		if (version.getStatus() != AnalysisVersionStatus.COMPLETED) {
			throw new IllegalArgumentException("생성이 완료된 분석만 PDF로 내려받을 수 있습니다.");
		}
		return documentService.getOrGenerate(
				AiTargetType.ANALYSIS, version.getId(), "pdf/analysis", version.getContent(),
				Map.of("versionNo", version.getVersionNo()));
	}

	/** [01] 9번 — 다중 선택 소프트 삭제. */
	@Transactional
	public void deleteAll(UUID workspaceId, UUID planningVersionId, UUID userId, List<UUID> analysisVersionIds) {
		planningVersionService.getOwned(planningVersionId, workspaceId, userId);
		List<AnalysisVersion> versions = analysisVersionRepository
				.findAllByIdInAndPlanningVersionIdAndDeletedAtIsNull(analysisVersionIds, planningVersionId);
		if (versions.size() != analysisVersionIds.size()) {
			throw new IllegalArgumentException("존재하지 않는 분석이 포함되어 있습니다.");
		}
		versions.forEach(AnalysisVersion::markDeleted);
	}

	// AiGenerationJobService가 커밋 이후 별도 Virtual Thread에서 실행하므로 트랜잭션/프록시 컨텍스트가 없다 —
	// PlanningVersionService.generate와 동일한 이유로 리포지토리 호출 자체의 트랜잭션 경계에 완료/실패 반영을 맡긴다.
	private void generate(UUID analysisVersionId, String planningContentJson) {
		try {
			PromptTemplate template = promptTemplateService.getActive(AiTargetType.ANALYSIS);
			AiGenerationRequest request = new AiGenerationRequest(
					template.getSystemPrompt(),
					planningContentJson,
					template.getToolName(),
					template.getToolDescription(),
					template.getSchemaJson());
			String content = aiClient.generateStructuredJson(request);
			completeVersion(analysisVersionId, content);
		} catch (RuntimeException e) {
			failVersion(analysisVersionId);
			throw e;
		}
	}

	private void completeVersion(UUID id, String content) {
		analysisVersionRepository.findById(id).ifPresent(version -> {
			version.complete(content);
			analysisVersionRepository.save(version);
		});
	}

	private void failVersion(UUID id) {
		analysisVersionRepository.findById(id).ifPresent(version -> {
			version.fail();
			analysisVersionRepository.save(version);
		});
	}
}
