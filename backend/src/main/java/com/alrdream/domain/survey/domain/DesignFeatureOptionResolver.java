package com.alrdream.domain.survey.domain;

import java.util.List;
import java.util.UUID;

/**
 * [02] §5-3 {@code DESIGN} 설문의 {@code core_feature_priority} 문항은 정적 옵션이 없고, 분석 단계 산출물에서
 * 동적으로 옵션이 생성된다. 분석 도메인(Phase 08)이 아직 없어 지금은 이 인터페이스만 두고 기본 구현({@code
 * NoAnalysisDesignFeatureOptionResolver})은 빈 목록을 반환한다 — 분석 도메인이 생기면 워크스페이스의 최신
 * 분석 산출물에서 실제 기능 후보를 읽어오는 구현체로 교체한다.
 */
public interface DesignFeatureOptionResolver {

	List<SurveySchema.Option> resolve(UUID workspaceId);
}
