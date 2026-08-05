package com.alrdream.domain.analysis.infrastructure;

import com.alrdream.domain.analysis.domain.AnalysisVersion;
import com.alrdream.domain.analysis.domain.AnalysisVersionRepository;
import com.alrdream.domain.analysis.domain.AnalysisVersionStatus;
import com.alrdream.domain.planning.domain.PlanningVersion;
import com.alrdream.domain.planning.domain.PlanningVersionRepository;
import com.alrdream.domain.survey.domain.DesignFeatureOptionResolver;
import com.alrdream.domain.survey.domain.SurveySchema;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * [02] §5-3 {@link DesignFeatureOptionResolver}의 실제 구현. 워크스페이스에 속한 기획안 버전들 중 가장 최근에
 * 완료된 분석의 {@code core_feature_candidates}를 DESIGN 설문 옵션으로 사용한다. 분석이 아직 없으면(또는 파싱에
 * 실패하면) 빈 목록을 반환한다 — {@link com.alrdream.domain.survey.application.SurveyAnswerValidator}가 빈
 * 옵션일 때는 값 검증을 건너뛰도록 이미 설계돼 있다.
 */
@Component
public class AnalysisFeatureOptionResolver implements DesignFeatureOptionResolver {

	private static final Logger log = LoggerFactory.getLogger(AnalysisFeatureOptionResolver.class);

	// SurveyDefinitionService 등과 동일한 이유(Jackson 2.x/3.x 혼재)로 독립 생성.
	private final ObjectMapper objectMapper = new ObjectMapper();

	private final PlanningVersionRepository planningVersionRepository;
	private final AnalysisVersionRepository analysisVersionRepository;

	public AnalysisFeatureOptionResolver(
			PlanningVersionRepository planningVersionRepository, AnalysisVersionRepository analysisVersionRepository) {
		this.planningVersionRepository = planningVersionRepository;
		this.analysisVersionRepository = analysisVersionRepository;
	}

	@Override
	public List<SurveySchema.Option> resolve(UUID workspaceId) {
		List<UUID> planningVersionIds = planningVersionRepository
				.findAllByWorkspaceIdAndDeletedAtIsNullOrderByVersionNoDesc(workspaceId)
				.stream()
				.map(PlanningVersion::getId)
				.toList();
		if (planningVersionIds.isEmpty()) {
			return List.of();
		}

		List<AnalysisVersion> completed = analysisVersionRepository
				.findAllByPlanningVersionIdInAndStatusAndDeletedAtIsNullOrderByCreatedAtDesc(
						planningVersionIds, AnalysisVersionStatus.COMPLETED);
		if (completed.isEmpty()) {
			return List.of();
		}

		return parseCandidates(completed.get(0));
	}

	private List<SurveySchema.Option> parseCandidates(AnalysisVersion analysisVersion) {
		try {
			JsonNode root = objectMapper.readTree(analysisVersion.getContent());
			JsonNode candidates = root.get("core_feature_candidates");
			if (candidates == null || !candidates.isArray()) {
				return List.of();
			}
			return objectMapper.convertValue(candidates, new TypeReference<List<SurveySchema.Option>>() {
			});
		} catch (JsonProcessingException | IllegalArgumentException e) {
			// 분석 결과 파싱 실패가 설문 조회 자체를 막아서는 안 된다 — 빈 옵션으로 폴백.
			log.warn("분석 결과에서 core_feature_candidates 파싱에 실패했습니다: analysisVersionId={}", analysisVersion.getId(), e);
			return List.of();
		}
	}
}
