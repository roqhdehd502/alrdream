package com.alrdream.domain.admin.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** Phase 16 — Admin 대시보드 상단 통계 카드. */
public record DashboardSummaryResponse(
		@Schema(description = "전체 가입자 수") long totalMembers,
		@Schema(description = "FREE 플랜 회원 수") long freeMembers,
		@Schema(description = "PRO 플랜 회원 수") long proMembers,
		@Schema(description = "이번 달(1일 0시~) AI 생성 시도 건수") long generationsThisMonth,
		@Schema(description = "이번 달 결제 성공 건수") long paymentsSucceededThisMonth,
		@Schema(description = "이번 달 결제 실패 건수") long paymentsFailedThisMonth) {
}
