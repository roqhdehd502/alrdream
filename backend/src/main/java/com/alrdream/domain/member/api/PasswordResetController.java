package com.alrdream.domain.member.api;

import com.alrdream.domain.member.api.dto.PasswordResetConfirmRequest;
import com.alrdream.domain.member.api.dto.PasswordResetRequestRequest;
import com.alrdream.domain.member.application.PasswordResetService;
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
@RequestMapping("/api/auth/password-reset")
public class PasswordResetController {

	private final PasswordResetService passwordResetService;

	public PasswordResetController(PasswordResetService passwordResetService) {
		this.passwordResetService = passwordResetService;
	}

	@Operation(
			summary = "비밀번호 재설정 코드 요청",
			description = "자체 가입(이메일+비밀번호) 계정의 이메일로 6자리 재설정 코드를 보낸다. 계정이 없거나 소셜 "
					+ "로그인 계정이어도 이메일 열거를 막기 위해 항상 204를 반환한다(실제 발송 여부는 응답으로 알 수 없음). "
					+ "같은 이메일로는 60초에 한 번만 요청할 수 있다.")
	@ApiResponse(responseCode = "204", description = "요청 접수(실제 발송 여부와 무관)")
	@ApiResponse(responseCode = "429", description = "너무 잦은 요청(같은 이메일 60초 이내 재요청)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@PostMapping("/request")
	public ResponseEntity<Void> request(@Valid @RequestBody PasswordResetRequestRequest request) {
		passwordResetService.requestReset(request.email());
		return ResponseEntity.noContent().build();
	}

	@Operation(
			summary = "비밀번호 재설정 확정",
			description = "이메일로 받은 코드와 새 비밀번호로 재설정을 확정한다. 성공 시 기존 로그인 세션(refresh token)은 "
					+ "모두 무효화되어 새 비밀번호로 다시 로그인해야 한다. 코드는 10분간 유효하며, 5회 틀리면 무효화된다.")
	@ApiResponse(responseCode = "204", description = "재설정 성공")
	@ApiResponse(responseCode = "400", description = "코드가 올바르지 않거나 만료됨",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@PostMapping("/confirm")
	public ResponseEntity<Void> confirm(@Valid @RequestBody PasswordResetConfirmRequest request) {
		passwordResetService.confirm(request.email(), request.code(), request.newPassword());
		return ResponseEntity.noContent().build();
	}
}
