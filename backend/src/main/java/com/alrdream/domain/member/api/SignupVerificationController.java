package com.alrdream.domain.member.api;

import com.alrdream.domain.member.api.dto.SignupVerificationConfirmRequest;
import com.alrdream.domain.member.api.dto.SignupVerificationRequestRequest;
import com.alrdream.domain.member.api.dto.SignupVerificationRequestResponse;
import com.alrdream.domain.member.application.SignupVerificationService;
import com.alrdream.global.error.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth", description = "회원가입, 로그인, 소셜 로그인, 토큰 갱신/로그아웃을 담당하는 인증 API")
@RestController
@RequestMapping("/api/auth/signup/email-verification")
public class SignupVerificationController {

	private final SignupVerificationService signupVerificationService;

	public SignupVerificationController(SignupVerificationService signupVerificationService) {
		this.signupVerificationService = signupVerificationService;
	}

	@Operation(
			summary = "회원가입 전 이메일 인증 코드 요청",
			description = "아직 계정을 만들기 전, 입력한 이메일로 6자리 인증 코드를 보낸다. 이미 가입된 이메일이면 "
					+ "400, 60초 이내 재요청이면 429를 반환한다.")
	@ApiResponse(responseCode = "200", description = "발송 성공, 코드 만료까지 남은 초를 반환")
	@ApiResponse(responseCode = "400", description = "이미 가입된 이메일",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@ApiResponse(responseCode = "429", description = "너무 잦은 요청(60초 이내 재요청)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@PostMapping("/request")
	public ResponseEntity<SignupVerificationRequestResponse> request(
			@Valid @RequestBody SignupVerificationRequestRequest request) {
		int expiresInSeconds = signupVerificationService.requestCode(request.email());
		return ResponseEntity.ok(new SignupVerificationRequestResponse(expiresInSeconds));
	}

	@Operation(
			summary = "회원가입 전 이메일 인증 코드 확인",
			description = "이메일로 받은 6자리 코드를 확인한다. 성공하면 30분 이내에 이 이메일로 "
					+ "POST /api/auth/signup을 호출해 가입을 완료해야 한다(만료되면 인증부터 다시 해야 함). "
					+ "코드는 10분간 유효하며, 5회 틀리면 무효화된다.")
	@ApiResponse(responseCode = "204", description = "인증 성공")
	@ApiResponse(responseCode = "400", description = "코드가 올바르지 않거나 만료됨",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@PostMapping("/confirm")
	public ResponseEntity<Void> confirm(@Valid @RequestBody SignupVerificationConfirmRequest request) {
		signupVerificationService.confirmCode(request.email(), request.code());
		return ResponseEntity.noContent().build();
	}
}
