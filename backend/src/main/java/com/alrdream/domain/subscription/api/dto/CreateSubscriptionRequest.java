package com.alrdream.domain.subscription.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record CreateSubscriptionRequest(
		@Schema(description = "Frontend/Admin이 PortOne SDK(requestIssueBillingKey)로 발급받은 빌링키 ID")
		@NotBlank String billingKeyId) {
}
