package com.alrdream.domain.survey.infrastructure;

import com.alrdream.domain.survey.domain.DesignFeatureOptionResolver;
import com.alrdream.domain.survey.domain.SurveySchema;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** {@link DesignFeatureOptionResolver}의 기본(placeholder) 구현 — 분석 도메인(Phase 08)이 생기기 전까지 항상 빈 목록. */
@Component
public class NoAnalysisDesignFeatureOptionResolver implements DesignFeatureOptionResolver {

	@Override
	public List<SurveySchema.Option> resolve(UUID workspaceId) {
		return List.of();
	}
}
