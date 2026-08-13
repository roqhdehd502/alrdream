package com.alrdream.domain.subscription.api.dto;

import com.alrdream.domain.subscription.domain.PaymentHistory;
import com.alrdream.domain.subscription.domain.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.UUID;

@Schema(description = "결제 내역 1건")
public record PaymentHistoryResponse(
		@Schema(description = "결제 내역 ID") UUID id,
		@Schema(description = "결제 금액(원)") long amount,
		@Schema(description = "결제 상태") PaymentStatus status,
		@Schema(description = "결제 승인 시각 — 실패 건은 null") OffsetDateTime paidAt,
		@Schema(description = "결제 시도 기록 시각") OffsetDateTime createdAt) {

	public static PaymentHistoryResponse of(PaymentHistory history) {
		return new PaymentHistoryResponse(
				history.getId(), history.getAmount(), history.getStatus(), history.getPaidAt(), history.getCreatedAt());
	}
}
