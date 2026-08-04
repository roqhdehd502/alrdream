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
import com.alrdream.global.security.MemberPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private final AuthService authService;
	private final MemberService memberService;

	public AuthController(AuthService authService, MemberService memberService) {
		this.authService = authService;
		this.memberService = memberService;
	}

	@PostMapping("/signup")
	public ResponseEntity<TokenResponse> signup(@Valid @RequestBody SignupRequest request) {
		return ResponseEntity.ok(toResponse(authService.signup(request.email(), request.password())));
	}

	@PostMapping("/login")
	public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
		return ResponseEntity.ok(toResponse(authService.login(request.email(), request.password())));
	}

	@PostMapping("/oauth/google")
	public ResponseEntity<TokenResponse> loginWithGoogle(@Valid @RequestBody OAuthLoginRequest request) {
		return ResponseEntity.ok(toResponse(authService.oauthLogin(AuthProvider.GOOGLE, request.idToken())));
	}

	@PostMapping("/oauth/apple")
	public ResponseEntity<TokenResponse> loginWithApple(@Valid @RequestBody OAuthLoginRequest request) {
		return ResponseEntity.ok(toResponse(authService.oauthLogin(AuthProvider.APPLE, request.idToken())));
	}

	@PostMapping("/refresh")
	public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshRequest request) {
		return ResponseEntity.ok(toResponse(authService.refresh(request.refreshToken())));
	}

	@PostMapping("/logout")
	public ResponseEntity<Void> logout(@AuthenticationPrincipal MemberPrincipal principal) {
		authService.logout(principal.memberId());
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/me")
	public ResponseEntity<MemberResponse> me(@AuthenticationPrincipal MemberPrincipal principal) {
		return ResponseEntity.ok(MemberResponse.from(memberService.getById(principal.memberId())));
	}

	private TokenResponse toResponse(TokenIssueResult result) {
		return new TokenResponse(result.accessToken(), result.refreshToken());
	}
}
