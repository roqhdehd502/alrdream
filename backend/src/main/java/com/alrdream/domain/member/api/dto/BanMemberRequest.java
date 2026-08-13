package com.alrdream.domain.member.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;

public record BanMemberRequest(
		@Schema(description = "영구 제재 여부 — true면 until은 무시된다") boolean permanent,
		@Schema(description = "일시 제재 해제 시각(permanent=false일 때 필수)") OffsetDateTime until) {
}
