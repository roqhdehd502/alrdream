package com.alrdream.domain.subscription.api.dto;

import com.alrdream.domain.member.domain.MemberPlan;
import com.alrdream.domain.subscription.domain.Subscription;
import com.alrdream.domain.subscription.domain.SubscriptionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.UUID;

public record SubscriptionAdminResponse(
		@Schema(description = "구독 ID") UUID id,
		@Schema(description = "사용자 ID") UUID userId,
		@Schema(description = "사용자 이메일") String userEmail,
		@Schema(description = "요금제") MemberPlan plan,
		@Schema(description = "상태") SubscriptionStatus status,
		@Schema(description = "다음 결제 예정일") OffsetDateTime nextBillingAt,
		@Schema(description = "구독 시작일") OffsetDateTime startedAt) {

	public static SubscriptionAdminResponse of(Subscription subscription, String userEmail) {
		return new SubscriptionAdminResponse(
				subscription.getId(), subscription.getUserId(), userEmail, subscription.getPlan(),
				subscription.getStatus(), subscription.getNextBillingAt(), subscription.getStartedAt());
	}
}
