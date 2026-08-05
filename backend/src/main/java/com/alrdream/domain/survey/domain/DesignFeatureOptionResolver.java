package com.alrdream.domain.survey.domain;

import java.util.List;
import java.util.UUID;

/**
 * [02] §5-3 {@code DESIGN} 설문의 {@code core_feature_priority} 문항은 정적 옵션이 없고, 분석 단계 산출물에서
 * 동적으로 옵션이 생성된다. 구현체는 {@code com.alrdream.domain.analysis.infrastructure.AnalysisFeatureOptionResolver}
 * (Phase 08) — 워크스페이스의 최신 완료된 분석 산출물에서 실제 기능 후보를 읽어온다.
 */
public interface DesignFeatureOptionResolver {

	List<SurveySchema.Option> resolve(UUID workspaceId);
}
