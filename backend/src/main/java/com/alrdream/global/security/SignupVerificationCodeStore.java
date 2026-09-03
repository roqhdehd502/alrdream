package com.alrdream.global.security;

import java.time.Duration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Phase 23 — 회원가입 전 이메일 인증 코드(Redis). 아직 계정이 없는 상태에서 이메일만으로 인증하므로
 * {@link PasswordResetCodeStore}와 같은 구조를 쓰되, 코드 확인에 성공하면 "이 이메일은 방금 인증을
 * 마쳤다"는 사실을 짧은 TTL의 별도 플래그({@link #markVerified})로 남겨 실제 가입(AuthService.signup)
 * 시점에 1회성으로 소비한다({@link #consumeVerified}) — 회원가입 정보 입력 단계 동안 인증 상태를 유지하기
 * 위함이다.
 */
@Component
public class SignupVerificationCodeStore {

	private static final String CODE_PREFIX = "signup-verification-code:";
	private static final String ATTEMPTS_PREFIX = "signup-verification-attempts:";
	private static final String COOLDOWN_PREFIX = "signup-verification-cooldown:";
	private static final String VERIFIED_PREFIX = "signup-verification-verified:";

	private final StringRedisTemplate redisTemplate;

	public SignupVerificationCodeStore(StringRedisTemplate redisTemplate) {
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

	/** 쿨다운을 원자적으로 시작한다(SETNX) — true면 이번 요청은 진행 가능, false면 이미 쿨다운 중(429). */
	public boolean tryStartCooldown(String email, long ttlSeconds) {
		Boolean success = redisTemplate.opsForValue()
				.setIfAbsent(COOLDOWN_PREFIX + email, "1", Duration.ofSeconds(ttlSeconds));
		return Boolean.TRUE.equals(success);
	}

	/** 코드 확인에 성공했을 때 호출 — 이후 {@code ttlSeconds} 동안은 이 이메일로 가입을 완료할 수 있다. */
	public void markVerified(String email, long ttlSeconds) {
		redisTemplate.opsForValue().set(VERIFIED_PREFIX + email, "1", Duration.ofSeconds(ttlSeconds));
	}

	/**
	 * 인증된 상태였는지 확인하고 동시에 소비(삭제)한다 — 같은 인증을 두 번 가입에 쓸 수 없게 1회용으로
	 * 만든다. Redis {@code DEL}은 단일 커맨드로 원자적이라 별도 락 없이도 동시 요청 중 하나만 성공한다.
	 */
	public boolean consumeVerified(String email) {
		Boolean deleted = redisTemplate.delete(VERIFIED_PREFIX + email);
		return Boolean.TRUE.equals(deleted);
	}
}
