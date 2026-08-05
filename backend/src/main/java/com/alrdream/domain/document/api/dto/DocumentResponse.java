package com.alrdream.domain.document.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;

@Schema(description = "PDF 다운로드 정보")
public record DocumentResponse(
		@Schema(description = "PDF 다운로드용 서명 URL (유효 시간 있음)") String downloadUrl,
		@Schema(description = "PDF가 최초 생성된 시각") OffsetDateTime generatedAt) {
}
