package com.alrdream.global.security;

import com.alrdream.domain.member.domain.MemberRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** [03] §4-5 자체 발급 Access/Refresh JWT의 생성·검증을 담당한다. */
@Component
public class JwtTokenProvider {

	private static final String CLAIM_ROLE = "role";
	private static final String CLAIM_TOKEN_TYPE = "tokenType";
	private static final String TOKEN_TYPE_ACCESS = "ACCESS";
	private static final String TOKEN_TYPE_REFRESH = "REFRESH";

	private final SecretKey key;
	private final long accessTokenValiditySeconds;
	private final long refreshTokenValiditySeconds;

	public JwtTokenProvider(
			@Value("${app.jwt.secret}") String secret,
			@Value("${app.jwt.access-token-validity-seconds}") long accessTokenValiditySeconds,
			@Value("${app.jwt.refresh-token-validity-seconds}") long refreshTokenValiditySeconds) {
		this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
		this.accessTokenValiditySeconds = accessTokenValiditySeconds;
		this.refreshTokenValiditySeconds = refreshTokenValiditySeconds;
	}

	public String createAccessToken(UUID memberId, MemberRole role) {
		return build(memberId, TOKEN_TYPE_ACCESS, accessTokenValiditySeconds, role);
	}

	public String createRefreshToken(UUID memberId) {
		return build(memberId, TOKEN_TYPE_REFRESH, refreshTokenValiditySeconds, null);
	}

	public long getRefreshTokenValiditySeconds() {
		return refreshTokenValiditySeconds;
	}

	private String build(UUID memberId, String tokenType, long validitySeconds, MemberRole role) {
		Instant now = Instant.now();
		var builder = Jwts.builder()
				.subject(memberId.toString())
				.claim(CLAIM_TOKEN_TYPE, tokenType)
				.issuedAt(Date.from(now))
				.expiration(Date.from(now.plusSeconds(validitySeconds)))
				.signWith(key);
		if (role != null) {
			builder.claim(CLAIM_ROLE, role.name());
		}
		return builder.compact();
	}

	/** 서명·만료를 검증하고 클레임을 반환한다. 유효하지 않으면 {@link JwtException}이 발생한다. */
	public Claims parse(String token) {
		return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
	}

	public UUID getMemberId(Claims claims) {
		return UUID.fromString(claims.getSubject());
	}

	public MemberRole getRole(Claims claims) {
		return MemberRole.valueOf(claims.get(CLAIM_ROLE, String.class));
	}

	public boolean isAccessToken(Claims claims) {
		return TOKEN_TYPE_ACCESS.equals(claims.get(CLAIM_TOKEN_TYPE, String.class));
	}

	public boolean isRefreshToken(Claims claims) {
		return TOKEN_TYPE_REFRESH.equals(claims.get(CLAIM_TOKEN_TYPE, String.class));
	}
}
