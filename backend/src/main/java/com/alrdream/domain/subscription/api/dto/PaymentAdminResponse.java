package com.alrdream.domain.subscription.api.dto;

import com.alrdream.domain.subscription.domain.PaymentHistory;
import com.alrdream.domain.subscription.domain.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PaymentAdminResponse(
		@Schema(description = "결제 이력 ID") UUID id,
		@Schema(description = "구독 ID") UUID subscriptionId,
		@Schema(description = "사용자 ID") UUID userId,
		@Schema(description = "사용자 이메일") String userEmail,
		@Schema(description = "결제 금액") long amount,
		@Schema(description = "상태") PaymentStatus status,
		@Schema(description = "결제 완료 시각") OffsetDateTime paidAt,
		@Schema(description = "결제 시도 기록 시각") OffsetDateTime createdAt) {

	public static PaymentAdminResponse of(PaymentHistory paymentHistory, UUID userId, String userEmail) {
		return new PaymentAdminResponse(
				paymentHistory.getId(), paymentHistory.getSubscriptionId(), userId, userEmail,
				paymentHistory.getAmount(), paymentHistory.getStatus(), paymentHistory.getPaidAt(),
				paymentHistory.getCreatedAt());
	}
}
