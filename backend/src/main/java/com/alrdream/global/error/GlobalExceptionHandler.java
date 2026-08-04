package com.alrdream.global.error;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException e) {
		return ResponseEntity.badRequest().body(new ErrorResponse("BAD_REQUEST", e.getMessage()));
	}

	// 매핑된 컨트롤러가 없는 경로 — catch-all Exception 핸들러가 가로채 500으로 뭉개기 전에 먼저 처리해 404를 유지한다.
	@ExceptionHandler(NoResourceFoundException.class)
	public ResponseEntity<ErrorResponse> handleNoResourceFound(NoResourceFoundException e) {
		return ResponseEntity.status(404).body(new ErrorResponse("NOT_FOUND", "요청한 리소스를 찾을 수 없습니다."));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleUnexpected(Exception e) {
		log.error("예기치 못한 오류가 발생했습니다.", e);
		return ResponseEntity.internalServerError()
				.body(new ErrorResponse("INTERNAL_ERROR", "예기치 못한 오류가 발생했습니다."));
	}

}
