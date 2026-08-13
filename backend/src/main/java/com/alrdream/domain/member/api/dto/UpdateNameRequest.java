package com.alrdream.domain.member.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

public record UpdateNameRequest(
		@Schema(description = "새 표시 이름. 빈 값이면 이름을 지우고 기본값(이메일)으로 되돌린다") @Size(max = 50) String name) {
}
