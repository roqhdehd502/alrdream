package com.alrdream.domain.member.application;

import com.alrdream.domain.member.domain.MemberRepository;
import com.alrdream.global.error.TooManyRequestsException;
import com.alrdream.global.mail.MailService;
import com.alrdream.global.mail.VerificationCodeEmailTemplate;
import com.alrdream.global.security.SignupVerificationCodeStore;
import java.security.SecureRandom;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Phase 23 — 회원가입 전 이메일 인증. "이메일 인증 → 회원가입 정보 입력 → 가입"순서로, 계정을 만들기 전에
 * 먼저 이메일 소유를 확인한다. 코드 발송/확인은 이메일만으로 하고(아직 로그인 상태가 아니므로), 확인에
 * 성공하면 {@link SignupVerificationCodeStore#markVerified}로 남긴 상태를 {@link AuthService#signup}이
 * {@link #consumeVerifiedEmail}로 1회성 소비해 실제 가입을 허용한다.
 */
@Service
@Transactional(readOnly = true)
public class SignupVerificationService {

	private static final int CODE_TTL_SECONDS = 600; // 10분
	private static final int COOLDOWN_SECONDS = 60;
	private static final int MAX_ATTEMPTS = 5;
	// 코드 확인 후 나머지 가입 폼(비밀번호 등)을 입력할 시간 — 코드 TTL보다 넉넉하게 잡는다.
	private static final int VERIFIED_TTL_SECONDS = 1800; // 30분

	private final MemberRepository memberRepository;
	private final SignupVerificationCodeStore codeStore;
	private final MailService mailService;
	private final SecureRandom random = new SecureRandom();

	public SignupVerificationService(
			MemberRepository memberRepository, SignupVerificationCodeStore codeStore, MailService mailService) {
		this.memberRepository = memberRepository;
		this.codeStore = codeStore;
		this.mailService = mailService;
	}

	/** @return 코드 만료까지 남은 초(프론트가 카운트다운을 표시하는 데 씀) */
	public int requestCode(String email) {
		if (memberRepository.existsByEmail(email)) {
			throw new IllegalArgumentException("이미 가입된 이메일입니다.");
		}
		if (!codeStore.tryStartCooldown(email, COOLDOWN_SECONDS)) {
			throw new TooManyRequestsException("잠시 후 다시 시도해주세요.", "TOO_MANY_REQUESTS");
		}
		String code = generateCode();
		codeStore.save(email, code, CODE_TTL_SECONDS);
		int ttlMinutes = CODE_TTL_SECONDS / 60;
		String description = "알려드림 회원가입을 위해 아래 코드를 입력해주세요.";
		mailService.sendHtml(
				email,
				"[알려드림] 회원가입 이메일 인증 코드",
				VerificationCodeEmailTemplate.text(description, code, ttlMinutes),
				VerificationCodeEmailTemplate.html("이메일 인증 코드", description, code, ttlMinutes));
		return CODE_TTL_SECONDS;
	}

	public void confirmCode(String email, String code) {
		if (!codeStore.matches(email, code)) {
			codeStore.recordFailedAttempt(email, MAX_ATTEMPTS);
			throw new IllegalArgumentException("코드가 올바르지 않거나 만료되었습니다.");
		}
		codeStore.invalidate(email);
		codeStore.markVerified(email, VERIFIED_TTL_SECONDS);
	}

	/** {@link AuthService#signup}에서만 호출 — 이 이메일이 방금 인증을 마쳤는지 확인하고 1회성으로 소비한다. */
	public boolean consumeVerifiedEmail(String email) {
		return codeStore.consumeVerified(email);
	}

	private String generateCode() {
		return String.format("%06d", random.nextInt(1_000_000));
	}
}
