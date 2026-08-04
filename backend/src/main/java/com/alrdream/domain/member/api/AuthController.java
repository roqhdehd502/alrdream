package com.alrdream.domain.member.api;

import com.alrdream.domain.member.api.dto.LoginRequest;
import com.alrdream.domain.member.api.dto.MemberResponse;
import com.alrdream.domain.member.api.dto.OAuthLoginRequest;
import com.alrdream.domain.member.api.dto.RefreshRequest;
import com.alrdream.domain.member.api.dto.SignupRequest;
import com.alrdream.domain.member.api.dto.TokenResponse;
import com.alrdream.domain.member.application.AuthService;
import com.alrdream.domain.member.application.AuthService.TokenIssueResult;
import com.alrdream.domain.member.application.MemberService;
import com.alrdream.domain.member.domain.AuthProvider;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth", description = "회원가입, 로그인, 소셜 로그인, 토큰 갱신/로그아웃을 담당하는 인증 API")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private final AuthService authService;
	private final MemberService memberService;

	public AuthController(AuthService authService, MemberService memberService) {
		this.authService = authService;
		this.memberService = memberService;
	}

	@Operation(
			summary = "이메일 회원가입",
			description = "이메일 + 비밀번호로 회원가입하고, 바로 사용 가능한 access/refresh 토큰을 발급한다.")
	@ApiResponse(responseCode = "200", description = "가입 성공, 토큰 발급 완료")
	@ApiResponse(responseCode = "400", description = "요청 형식 오류 또는 이미 가입된 이메일",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@PostMapping("/signup")
	public ResponseEntity<TokenResponse> signup(@Valid @RequestBody SignupRequest request) {
		return ResponseEntity.ok(toResponse(authService.signup(request.email(), request.password())));
	}

	@Operation(summary = "이메일 로그인", description = "이메일 + 비밀번호로 로그인하고 access/refresh 토큰을 발급한다.")
	@ApiResponse(responseCode = "200", description = "로그인 성공, 토큰 발급 완료")
	@ApiResponse(responseCode = "400", description = "이메일 또는 비밀번호가 올바르지 않음",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@PostMapping("/login")
	public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
		return ResponseEntity.ok(toResponse(authService.login(request.email(), request.password())));
	}

	@Operation(
			summary = "Google 소셜 로그인",
			description = "Frontend가 Google SDK로 발급받은 ID 토큰을 서버가 검증해 로그인/자동 가입 처리 후 access/refresh 토큰을 발급한다.")
	@ApiResponse(responseCode = "200", description = "로그인 성공, 토큰 발급 완료")
	@ApiResponse(responseCode = "400", description = "ID 토큰이 유효하지 않거나 다른 방식으로 가입된 이메일과 충돌",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@PostMapping("/oauth/google")
	public ResponseEntity<TokenResponse> loginWithGoogle(@Valid @RequestBody OAuthLoginRequest request) {
		return ResponseEntity.ok(toResponse(authService.oauthLogin(AuthProvider.GOOGLE, request.idToken())));
	}

	@Operation(
			summary = "Apple 소셜 로그인",
			description = "Frontend가 Apple SDK로 발급받은 identityToken을 서버가 검증해 로그인/자동 가입 처리 후 access/refresh 토큰을 발급한다.")
	@ApiResponse(responseCode = "200", description = "로그인 성공, 토큰 발급 완료")
	@ApiResponse(responseCode = "400", description = "ID 토큰이 유효하지 않거나 다른 방식으로 가입된 이메일과 충돌",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@PostMapping("/oauth/apple")
	public ResponseEntity<TokenResponse> loginWithApple(@Valid @RequestBody OAuthLoginRequest request) {
		return ResponseEntity.ok(toResponse(authService.oauthLogin(AuthProvider.APPLE, request.idToken())));
	}

	@Operation(
			summary = "토큰 갱신",
			description = "유효한 refresh token으로 access/refresh 토큰을 모두 새로 발급한다(로테이션). 기존 refresh token은 즉시 무효화된다.")
	@ApiResponse(responseCode = "200", description = "갱신 성공, 새 토큰 발급 완료")
	@ApiResponse(responseCode = "400", description = "refresh token이 만료/무효화되었거나 형식이 올바르지 않음",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@PostMapping("/refresh")
	public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshRequest request) {
		return ResponseEntity.ok(toResponse(authService.refresh(request.refreshToken())));
	}

	@Operation(summary = "로그아웃", description = "현재 회원의 refresh token을 무효화한다. 이후 해당 refresh token으로는 갱신할 수 없다.")
	@ApiResponse(responseCode = "204", description = "로그아웃 성공")
	@ApiResponse(responseCode = "401", description = "인증되지 않은 요청 (access token 없음/만료)")
	@SecurityRequirement(name = "bearerAuth")
	@PostMapping("/logout")
	public ResponseEntity<Void> logout(@AuthenticationPrincipal MemberPrincipal principal) {
		authService.logout(principal.memberId());
		return ResponseEntity.noContent().build();
	}

	@Operation(summary = "내 정보 조회", description = "access token으로 인증된 현재 회원의 정보(이메일, role, 요금제)를 조회한다.")
	@ApiResponse(responseCode = "200", description = "조회 성공")
	@ApiResponse(responseCode = "401", description = "인증되지 않은 요청 (access token 없음/만료)")
	@SecurityRequirement(name = "bearerAuth")
	@GetMapping("/me")
	public ResponseEntity<MemberResponse> me(@AuthenticationPrincipal MemberPrincipal principal) {
		return ResponseEntity.ok(MemberResponse.from(memberService.getById(principal.memberId())));
	}

	private TokenResponse toResponse(TokenIssueResult result) {
		return new TokenResponse(result.accessToken(), result.refreshToken());
	}
}
