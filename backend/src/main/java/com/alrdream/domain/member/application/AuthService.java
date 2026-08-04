package com.alrdream.domain.member.application;

import com.alrdream.domain.member.domain.AuthProvider;
import com.alrdream.domain.member.domain.Member;
import com.alrdream.domain.member.domain.MemberRepository;
import com.alrdream.domain.member.infrastructure.AppleIdTokenVerifierAdapter;
import com.alrdream.domain.member.infrastructure.GoogleIdTokenVerifierAdapter;
import com.alrdream.domain.member.infrastructure.OAuthUserInfo;
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

	public AuthService(
			MemberRepository memberRepository,
			PasswordEncoder passwordEncoder,
			JwtTokenProvider jwtTokenProvider,
			RefreshTokenStore refreshTokenStore,
			GoogleIdTokenVerifierAdapter googleIdTokenVerifier,
			AppleIdTokenVerifierAdapter appleIdTokenVerifier) {
		this.memberRepository = memberRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtTokenProvider = jwtTokenProvider;
		this.refreshTokenStore = refreshTokenStore;
		this.googleIdTokenVerifier = googleIdTokenVerifier;
		this.appleIdTokenVerifier = appleIdTokenVerifier;
	}

	@Transactional
	public TokenIssueResult signup(String email, String rawPassword) {
		if (memberRepository.existsByEmail(email)) {
			throw new IllegalArgumentException("이미 가입된 이메일입니다.");
		}
		Member member = Member.createLocal(email, passwordEncoder.encode(rawPassword));
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
		return issueTokens(member);
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
		return issueTokens(member);
	}

	@Transactional
	public void logout(UUID memberId) {
		refreshTokenStore.invalidate(memberId);
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
