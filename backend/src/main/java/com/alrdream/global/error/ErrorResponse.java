package com.alrdream.global.error;

import io.swagger.v3.oas.annotations.media.Schema;

public record ErrorResponse(
		@Schema(description = "오류 코드", example = "BAD_REQUEST") String code,
		@Schema(description = "오류 메시지", example = "요청 형식이 올바르지 않습니다.") String message) {
}
