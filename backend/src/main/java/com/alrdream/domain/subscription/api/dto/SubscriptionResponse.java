package com.alrdream.domain.subscription.api.dto;

import com.alrdream.domain.member.domain.MemberPlan;
import com.alrdream.domain.subscription.domain.Subscription;
import com.alrdream.domain.subscription.domain.SubscriptionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.UUID;

@Schema(description = "구독 상태")
public record SubscriptionResponse(
		@Schema(description = "구독 ID") UUID id,
		@Schema(description = "플랜") MemberPlan plan,
		@Schema(description = "상태 — 최초 결제가 웹훅으로 확정되기 전까지는 PAST_DUE") SubscriptionStatus status,
		@Schema(description = "다음 결제 예정 시각") OffsetDateTime nextBillingAt,
		@Schema(description = "구독 시작 시각") OffsetDateTime startedAt) {

	public static SubscriptionResponse of(Subscription subscription) {
		return new SubscriptionResponse(
				subscription.getId(),
				subscription.getPlan(),
				subscription.getStatus(),
				subscription.getNextBillingAt(),
				subscription.getStartedAt());
	}
}
