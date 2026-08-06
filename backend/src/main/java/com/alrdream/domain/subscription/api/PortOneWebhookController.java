package com.alrdream.domain.subscription.api;

import com.alrdream.domain.subscription.application.PortOneWebhookService;
import com.alrdream.global.error.ErrorResponse;
import io.portone.sdk.server.errors.WebhookVerificationException;
import io.portone.sdk.server.webhook.WebhookVerifier;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * [03] §4-7 — PortOne 웹훅 수신 엔드포인트. PortOne이 우리 JWT를 알 수 없으므로 인증 대신 Standard Webhooks
 * 서명(HMAC)으로만 검증한다({@code SecurityConfig}의 permitAll 목록 참고). PortOne이 실패 시 최대 5회 지수
 * 백오프로 재전송하므로, 처리 중 예기치 못한 오류(5xx)가 나면 그대로 재시도되게 두는 편이 안전하다 — 여기서는
 * 서명 검증 실패(400)만 명시적으로 구분해 응답한다.
 */
@Tag(name = "Webhook", description = "PortOne 결제 웹훅 (인증 없음, 서명 검증)")
@RestController
@RequestMapping("/webhooks/portone")
public class PortOneWebhookController {

	private static final Logger log = LoggerFactory.getLogger(PortOneWebhookController.class);

	private final PortOneWebhookService portOneWebhookService;

	public PortOneWebhookController(PortOneWebhookService portOneWebhookService) {
		this.portOneWebhookService = portOneWebhookService;
	}

	@Operation(
			summary = "PortOne 웹훅 수신",
			description = "Transaction.Paid/Failed 이벤트를 처리한다. payment_id 기준으로 멱등 처리하므로 중복 수신에도 안전하다.")
	@ApiResponse(responseCode = "200", description = "처리 완료 (이미 처리된 이벤트 포함)")
	@ApiResponse(responseCode = "400", description = "웹훅 서명 검증 실패",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@PostMapping
	public ResponseEntity<Void> handle(
			@RequestBody String rawBody,
			@RequestHeader(WebhookVerifier.HEADER_ID) String webhookId,
			@RequestHeader(WebhookVerifier.HEADER_SIGNATURE) String signature,
			@RequestHeader(WebhookVerifier.HEADER_TIMESTAMP) String timestamp) {
		try {
			portOneWebhookService.handle(rawBody, webhookId, signature, timestamp);
			return ResponseEntity.ok().build();
		} catch (WebhookVerificationException e) {
			log.warn("PortOne 웹훅 서명 검증에 실패했습니다.", e);
			return ResponseEntity.badRequest().build();
		}
	}
}
