package com.alrdream.global.security;

import java.time.Duration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Phase 16 — 비밀번호 재설정 인증 코드(Redis). {@link RefreshTokenStore}와 같은 저장소를 재사용해 별도
 * 테이블 없이 짧은 TTL로 관리한다. 이메일 열거 공격 방지를 위해 계정 존재 여부와 무관하게 쿨다운은 항상
 * 적용된다({@link #tryStartCooldown}).
 */
@Component
public class PasswordResetCodeStore {

	private static final String CODE_PREFIX = "password-reset-code:";
	private static final String ATTEMPTS_PREFIX = "password-reset-attempts:";
	private static final String COOLDOWN_PREFIX = "password-reset-cooldown:";

	private final StringRedisTemplate redisTemplate;

	public PasswordResetCodeStore(StringRedisTemplate redisTemplate) {
		this.redisTemplate = redisTemplate;
	}

	public void save(String email, String code, long ttlSeconds) {
		redisTemplate.opsForValue().set(CODE_PREFIX + email, code, Duration.ofSeconds(ttlSeconds));
		redisTemplate.delete(ATTEMPTS_PREFIX + email);
	}

	public boolean matches(String email, String code) {
		String stored = redisTemplate.opsForValue().get(CODE_PREFIX + email);
		return stored != null && stored.equals(code);
	}

	/** 틀린 시도 횟수를 늘리고, 한도를 넘으면 코드 자체를 무효화해 새로 요청하도록 강제한다(6자리 브루트포스 방지). */
	public void recordFailedAttempt(String email, int maxAttempts) {
		String key = ATTEMPTS_PREFIX + email;
		Long attempts = redisTemplate.opsForValue().increment(key);
		redisTemplate.expire(key, Duration.ofSeconds(600));
		if (attempts != null && attempts >= maxAttempts) {
			invalidate(email);
		}
	}

	public void invalidate(String email) {
		redisTemplate.delete(CODE_PREFIX + email);
		redisTemplate.delete(ATTEMPTS_PREFIX + email);
	}

	/**
	 * 쿨다운을 원자적으로 시작한다(SETNX) — true면 이번 요청은 진행 가능, false면 이미 쿨다운 중(429).
	 * 계정 존재 여부와 무관하게 항상 호출되므로, 이 결과만으로는 이메일 존재 여부를 알 수 없다.
	 */
	public boolean tryStartCooldown(String email, long ttlSeconds) {
		Boolean success = redisTemplate.opsForValue()
				.setIfAbsent(COOLDOWN_PREFIX + email, "1", Duration.ofSeconds(ttlSeconds));
		return Boolean.TRUE.equals(success);
	}
}
