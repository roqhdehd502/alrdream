package com.alrdream.domain.member.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record RefreshRequest(@Schema(description = "발급받은 refresh token") @NotBlank String refreshToken) {
}
