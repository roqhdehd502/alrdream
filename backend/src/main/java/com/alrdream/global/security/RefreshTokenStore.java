package com.alrdream.global.security;

import java.time.Duration;
import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * [03] §4-5 Refresh Token 저장/무효화(Redis). 회원 1명당 활성 세션 1개를 가정한다 —
 * 새로 로그인하면 이전 refresh token은 자동으로 무효화된다(같은 key를 덮어씀).
 */
@Component
public class RefreshTokenStore {

	private static final String KEY_PREFIX = "refresh-token:";

	private final StringRedisTemplate redisTemplate;

	public RefreshTokenStore(StringRedisTemplate redisTemplate) {
		this.redisTemplate = redisTemplate;
	}

	public void save(UUID memberId, String refreshToken, long validitySeconds) {
		redisTemplate.opsForValue().set(key(memberId), refreshToken, Duration.ofSeconds(validitySeconds));
	}

	/** 저장된 refresh token과 일치하는지 확인한다 (로그아웃되었거나 새 로그인으로 교체된 토큰은 불일치로 처리). */
	public boolean matches(UUID memberId, String refreshToken) {
		String stored = redisTemplate.opsForValue().get(key(memberId));
		return stored != null && stored.equals(refreshToken);
	}

	public void invalidate(UUID memberId) {
		redisTemplate.delete(key(memberId));
	}

	private String key(UUID memberId) {
		return KEY_PREFIX + memberId;
	}
}
