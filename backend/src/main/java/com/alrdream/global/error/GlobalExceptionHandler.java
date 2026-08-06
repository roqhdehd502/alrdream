package com.alrdream.global.error;

import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException e) {
		return ResponseEntity.badRequest().body(new ErrorResponse("BAD_REQUEST", e.getMessage()));
	}

	// @Valid로 검증되는 요청 DTO(record)가 실패했을 때 — 이것도 다른 핸들러 없이는 catch-all에 걸려 500이 된다.
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
		String message = e.getBindingResult().getFieldErrors().stream()
				.map(error -> error.getField() + ": " + error.getDefaultMessage())
				.collect(Collectors.joining(", "));
		return ResponseEntity.badRequest().body(new ErrorResponse("BAD_REQUEST", message));
	}

	// 경로/쿼리 변수 타입 변환 실패(예: UUID 형식이 아닌 workspaceId, enum에 없는 surveyKey) — 역시 처리 안 하면 500.
	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
		String message = e.getName() + " 값이 올바르지 않습니다: " + e.getValue();
		return ResponseEntity.badRequest().body(new ErrorResponse("BAD_REQUEST", message));
	}

	// PortOneWebhookController처럼 @RequestHeader가 필수인데 헤더가 누락된 경우 — 처리 안 하면 500(PortOne이
	// 형식이 다른 테스트 호출을 보내는 경우 등).
	@ExceptionHandler(MissingRequestHeaderException.class)
	public ResponseEntity<ErrorResponse> handleMissingHeader(MissingRequestHeaderException e) {
		return ResponseEntity.badRequest()
				.body(new ErrorResponse("BAD_REQUEST", "필수 헤더가 누락되었습니다: " + e.getHeaderName()));
	}

	// 매핑된 컨트롤러가 없는 경로 — catch-all Exception 핸들러가 가로채 500으로 뭉개기 전에 먼저 처리해 404를 유지한다.
	@ExceptionHandler(NoResourceFoundException.class)
	public ResponseEntity<ErrorResponse> handleNoResourceFound(NoResourceFoundException e) {
		return ResponseEntity.status(404).body(new ErrorResponse("NOT_FOUND", "요청한 리소스를 찾을 수 없습니다."));
	}

	// 깨진 JSON, 필드 타입이 안 맞는 JSON(예: 배열 자리에 문자열) — @RequestBody 파싱 단계 실패.
	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ErrorResponse> handleNotReadable(HttpMessageNotReadableException e) {
		return ResponseEntity.badRequest().body(new ErrorResponse("BAD_REQUEST", "요청 본문을 읽을 수 없습니다."));
	}

	// 매핑된 경로지만 지원하지 않는 HTTP 메서드로 호출한 경우.
	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
		return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
				.body(new ErrorResponse("METHOD_NOT_ALLOWED", "지원하지 않는 HTTP 메서드입니다: " + e.getMethod()));
	}

	// Content-Type이 없거나 지원하지 않는 경우(예: JSON 바디인데 Content-Type 미지정 시 폼 데이터로 오인).
	@ExceptionHandler(HttpMediaTypeNotSupportedException.class)
	public ResponseEntity<ErrorResponse> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException e) {
		return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
				.body(new ErrorResponse("UNSUPPORTED_MEDIA_TYPE", "지원하지 않는 Content-Type입니다."));
	}

	// [01] 13번 — FREE 플랜 월별 AI 생성 횟수 한도 초과.
	@ExceptionHandler(TooManyRequestsException.class)
	public ResponseEntity<ErrorResponse> handleTooManyRequests(TooManyRequestsException e) {
		return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
				.body(new ErrorResponse("QUOTA_EXCEEDED", e.getMessage()));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleUnexpected(Exception e) {
		log.error("예기치 못한 오류가 발생했습니다.", e);
		return ResponseEntity.internalServerError()
				.body(new ErrorResponse("INTERNAL_ERROR", "예기치 못한 오류가 발생했습니다."));
	}

}
