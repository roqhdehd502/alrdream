package com.alrdream.domain.subscription.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;

public record SetPromotionRequest(
		@Schema(description = "프로모션 기간 중 청구 금액(원)") @Min(0) long promoPriceKrw,
		@Schema(description = "프로모션 시작 시각") @NotNull OffsetDateTime promoStartsAt,
		@Schema(description = "프로모션 종료 시각") @NotNull OffsetDateTime promoEndsAt) {
}
