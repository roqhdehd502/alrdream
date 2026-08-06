package com.alrdream.domain.subscription.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** [03] §2-1 Admin 대시보드 — 상태별 구독 수 요약. */
public record SubscriptionSummaryResponse(
		@Schema(description = "정상 결제 중인 구독 수") long activeCount,
		@Schema(description = "결제 실패/미확정 구독 수") long pastDueCount,
		@Schema(description = "해지된 구독 수") long canceledCount) {
}
