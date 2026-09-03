package com.alrdream.domain.member.application;

import com.alrdream.domain.member.domain.AuthProvider;
import com.alrdream.domain.member.domain.Member;
import com.alrdream.domain.member.domain.MemberRepository;
import com.alrdream.domain.member.infrastructure.AppleIdTokenVerifierAdapter;
import com.alrdream.domain.member.infrastructure.GoogleIdTokenVerifierAdapter;
import com.alrdream.domain.member.infrastructure.OAuthUserInfo;
import com.alrdream.global.error.ForbiddenException;
import com.alrdream.global.security.JwtTokenProvider;
import com.alrdream.global.security.RefreshTokenStore;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AuthService {

	private final MemberRepository memberRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtTokenProvider jwtTokenProvider;
	private final RefreshTokenStore refreshTokenStore;
	private final GoogleIdTokenVerifierAdapter googleIdTokenVerifier;
	private final AppleIdTokenVerifierAdapter appleIdTokenVerifier;
	private final SignupVerificationService signupVerificationService;

	public AuthService(
			MemberRepository memberRepository,
			PasswordEncoder passwordEncoder,
			JwtTokenProvider jwtTokenProvider,
			RefreshTokenStore refreshTokenStore,
			GoogleIdTokenVerifierAdapter googleIdTokenVerifier,
			AppleIdTokenVerifierAdapter appleIdTokenVerifier,
			SignupVerificationService signupVerificationService) {
		this.memberRepository = memberRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtTokenProvider = jwtTokenProvider;
		this.refreshTokenStore = refreshTokenStore;
		this.googleIdTokenVerifier = googleIdTokenVerifier;
		this.appleIdTokenVerifier = appleIdTokenVerifier;
		this.signupVerificationService = signupVerificationService;
	}

	/**
	 * Phase 23 — 이메일 인증(SignupVerificationService) → 정보 입력 → 가입 순서를 강제한다. 이 이메일로
	 * {@code SignupVerificationService.confirmCode}를 먼저 통과하지 않으면 가입 자체가 거부된다.
	 */
	@Transactional
	public TokenIssueResult signup(String email, String rawPassword) {
		if (memberRepository.existsByEmail(email)) {
			throw new IllegalArgumentException("이미 가입된 이메일입니다.");
		}
		if (!signupVerificationService.consumeVerifiedEmail(email)) {
			throw new IllegalArgumentException("이메일 인증이 필요합니다.");
		}
		Member member = Member.createLocal(email, passwordEncoder.encode(rawPassword));
		member.markEmailVerified();
		memberRepository.save(member);
		return issueTokens(member);
	}

	public TokenIssueResult login(String email, String rawPassword) {
		Member member = memberRepository.findByEmail(email)
				.orElseThrow(() -> new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다."));
		if (member.getProvider() != AuthProvider.LOCAL
				|| !passwordEncoder.matches(rawPassword, member.getPasswordHash())) {
			throw new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다.");
		}
		checkNotBanned(member);
		return issueTokens(member);
	}

	/**
	 * 이미 access token으로 인증된 사용자가 민감한 작업(예: 회원탈퇴) 전에 비밀번호만 재확인할 때 사용 —
	 * {@link #login}과 달리 새 토큰을 발급하지 않아 {@link RefreshTokenStore}를 건드리지 않는다(회원당 refresh
	 * token을 하나만 유지하는 구조라, login()을 재확인 용도로 재사용하면 기존 세션의 refresh token이 새
	 * 토큰으로 덮어써져 액세스 토큰 만료 후 자동 갱신이 끊기는 문제가 있었다 — Phase 21 전수 점검에서 발견).
	 */
	public void verifyPassword(UUID memberId, String rawPassword) {
		Member member = memberRepository.findById(memberId)
				.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));
		if (member.getProvider() != AuthProvider.LOCAL
				|| !passwordEncoder.matches(rawPassword, member.getPasswordHash())) {
			throw new IllegalArgumentException("비밀번호가 올바르지 않습니다.");
		}
	}

	@Transactional
	public TokenIssueResult oauthLogin(AuthProvider provider, String idToken) {
		OAuthUserInfo userInfo = switch (provider) {
			case GOOGLE -> googleIdTokenVerifier.verify(idToken);
			case APPLE -> appleIdTokenVerifier.verify(idToken);
			case LOCAL -> throw new IllegalArgumentException("지원하지 않는 provider입니다.");
		};

		Member member = memberRepository.findByProviderAndProviderId(provider, userInfo.providerId())
				.orElseGet(() -> registerOAuthMember(provider, userInfo));
		checkNotBanned(member);
		return issueTokens(member);
	}

	private Member registerOAuthMember(AuthProvider provider, OAuthUserInfo userInfo) {
		if (memberRepository.existsByEmail(userInfo.email())) {
			throw new IllegalArgumentException("이미 다른 방식으로 가입된 이메일입니다.");
		}
		Member member = Member.createOAuth(userInfo.email(), provider, userInfo.providerId());
		return memberRepository.save(member);
	}

	@Transactional
	public TokenIssueResult refresh(String refreshToken) {
		Claims claims;
		try {
			claims = jwtTokenProvider.parse(refreshToken);
		} catch (JwtException | IllegalArgumentException e) {
			throw new IllegalArgumentException("유효하지 않은 refresh token입니다.");
		}
		if (!jwtTokenProvider.isRefreshToken(claims)) {
			throw new IllegalArgumentException("refresh token이 아닙니다.");
		}

		UUID memberId = jwtTokenProvider.getMemberId(claims);
		if (!refreshTokenStore.matches(memberId, refreshToken)) {
			throw new IllegalArgumentException("만료되었거나 무효화된 refresh token입니다.");
		}

		Member member = memberRepository.findById(memberId)
				.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));
		checkNotBanned(member);
		return issueTokens(member);
	}

	@Transactional
	public void logout(UUID memberId) {
		refreshTokenStore.invalidate(memberId);
	}

	/** Phase 19 — 제재된 계정은 이미 유효한 자격증명/refresh token이 있어도 새 토큰을 발급받을 수 없다. */
	private void checkNotBanned(Member member) {
		if (member.isBanned()) {
			throw new ForbiddenException(member.banMessage(), "ACCOUNT_BANNED");
		}
	}

	private TokenIssueResult issueTokens(Member member) {
		String accessToken = jwtTokenProvider.createAccessToken(member.getId(), member.getRole());
		String refreshToken = jwtTokenProvider.createRefreshToken(member.getId());
		refreshTokenStore.save(member.getId(), refreshToken, jwtTokenProvider.getRefreshTokenValiditySeconds());
		return new TokenIssueResult(accessToken, refreshToken);
	}

	public record TokenIssueResult(String accessToken, String refreshToken) {
	}
}
