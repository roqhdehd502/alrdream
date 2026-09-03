package com.alrdream.global.security;

import java.time.Duration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Phase 23 — 이메일 인증 코드(Redis). {@link PasswordResetCodeStore}와 동일한 구조로, 회원 ID를 키로
 * 짧은 TTL로 관리한다(별도 테이블 불필요). 인증은 로그인된 회원 본인만 요청할 수 있어 비밀번호 재설정과
 * 달리 이메일 열거 공격을 고려할 필요가 없다.
 */
@Component
public class EmailVerificationCodeStore {

	private static final String CODE_PREFIX = "email-verification-code:";
	private static final String ATTEMPTS_PREFIX = "email-verification-attempts:";
	private static final String COOLDOWN_PREFIX = "email-verification-cooldown:";

	private final StringRedisTemplate redisTemplate;

	public EmailVerificationCodeStore(StringRedisTemplate redisTemplate) {
		this.redisTemplate = redisTemplate;
	}

	public void save(String memberId, String code, long ttlSeconds) {
		redisTemplate.opsForValue().set(CODE_PREFIX + memberId, code, Duration.ofSeconds(ttlSeconds));
		redisTemplate.delete(ATTEMPTS_PREFIX + memberId);
	}

	public boolean matches(String memberId, String code) {
		String stored = redisTemplate.opsForValue().get(CODE_PREFIX + memberId);
		return stored != null && stored.equals(code);
	}

	/** 틀린 시도 횟수를 늘리고, 한도를 넘으면 코드 자체를 무효화해 새로 요청하도록 강제한다(6자리 브루트포스 방지). */
	public void recordFailedAttempt(String memberId, int maxAttempts) {
		String key = ATTEMPTS_PREFIX + memberId;
		Long attempts = redisTemplate.opsForValue().increment(key);
		redisTemplate.expire(key, Duration.ofSeconds(600));
		if (attempts != null && attempts >= maxAttempts) {
			invalidate(memberId);
		}
	}

	public void invalidate(String memberId) {
		redisTemplate.delete(CODE_PREFIX + memberId);
		redisTemplate.delete(ATTEMPTS_PREFIX + memberId);
	}

	/** 쿨다운을 원자적으로 시작한다(SETNX) — true면 이번 요청은 진행 가능, false면 이미 쿨다운 중(429). */
	public boolean tryStartCooldown(String memberId, long ttlSeconds) {
		Boolean success = redisTemplate.opsForValue()
				.setIfAbsent(COOLDOWN_PREFIX + memberId, "1", Duration.ofSeconds(ttlSeconds));
		return Boolean.TRUE.equals(success);
	}
}
