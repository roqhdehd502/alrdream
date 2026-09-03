package com.alrdream.domain.member.api;

import com.alrdream.domain.member.api.dto.EmailVerificationConfirmRequest;
import com.alrdream.domain.member.api.dto.EmailVerificationRequestResponse;
import com.alrdream.domain.member.application.EmailVerificationService;
import com.alrdream.global.error.ErrorResponse;
import com.alrdream.global.security.MemberPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth", description = "회원가입, 로그인, 소셜 로그인, 토큰 갱신/로그아웃을 담당하는 인증 API")
@RestController
@RequestMapping("/api/auth/email-verification")
public class EmailVerificationController {

	private final EmailVerificationService emailVerificationService;

	public EmailVerificationController(EmailVerificationService emailVerificationService) {
		this.emailVerificationService = emailVerificationService;
	}

	@Operation(
			summary = "이메일 인증 코드 요청",
			description = "로그인된 본인의 이메일로 6자리 인증 코드를 보낸다. 이미 인증된 계정이면 400, 60초 이내 "
					+ "재요청이면 429를 반환한다.")
	@ApiResponse(responseCode = "200", description = "발송 성공, 코드 만료까지 남은 초를 반환")
	@ApiResponse(responseCode = "400", description = "이미 인증된 계정",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@ApiResponse(responseCode = "429", description = "너무 잦은 요청(60초 이내 재요청)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@ApiResponse(responseCode = "401", description = "인증되지 않은 요청 (access token 없음/만료)")
	@SecurityRequirement(name = "bearerAuth")
	@PostMapping("/request")
	public ResponseEntity<EmailVerificationRequestResponse> request(
			@AuthenticationPrincipal MemberPrincipal principal) {
		int expiresInSeconds = emailVerificationService.requestCode(principal.memberId());
		return ResponseEntity.ok(new EmailVerificationRequestResponse(expiresInSeconds));
	}

	@Operation(
			summary = "이메일 인증 코드 확인",
			description = "이메일로 받은 6자리 코드를 확인해 계정을 인증 완료 처리한다. 코드는 10분간 유효하며, "
					+ "5회 틀리면 무효화되어 새로 요청해야 한다.")
	@ApiResponse(responseCode = "204", description = "인증 성공")
	@ApiResponse(responseCode = "400", description = "코드가 올바르지 않거나 만료됨",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@ApiResponse(responseCode = "401", description = "인증되지 않은 요청 (access token 없음/만료)")
	@SecurityRequirement(name = "bearerAuth")
	@PostMapping("/confirm")
	public ResponseEntity<Void> confirm(
			@AuthenticationPrincipal MemberPrincipal principal,
			@Valid @RequestBody EmailVerificationConfirmRequest request) {
		emailVerificationService.confirmCode(principal.memberId(), request.code());
		return ResponseEntity.noContent().build();
	}
}
