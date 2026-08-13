package com.alrdream.domain.ai.api.dto;

import com.alrdream.domain.ai.application.UsageQuotaService.UsageQuotaSnapshot;
import com.alrdream.domain.member.domain.MemberPlan;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "내 이번 달 AI 생성 사용량 — [01] 13번 생성 횟수 제한(Phase 20부터 FREE/PRO 모두 적용)")
public record UsageQuotaResponse(
		@Schema(description = "조회 기준 기간(YYYY-MM)") String period,
		@Schema(description = "이번 달 생성 횟수") int generationCount,
		@Schema(description = "이번 달 한도") int limitCount,
		@Schema(description = "현재 플랜") MemberPlan plan) {

	public static UsageQuotaResponse of(UsageQuotaSnapshot snapshot) {
		return new UsageQuotaResponse(snapshot.period(), snapshot.generationCount(), snapshot.limitCount(), snapshot.plan());
	}
}
