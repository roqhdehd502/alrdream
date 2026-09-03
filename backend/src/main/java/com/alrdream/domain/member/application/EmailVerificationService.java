package com.alrdream.domain.member.application;

import com.alrdream.domain.member.domain.Member;
import com.alrdream.domain.member.domain.MemberRepository;
import com.alrdream.global.error.TooManyRequestsException;
import com.alrdream.global.mail.MailService;
import com.alrdream.global.mail.VerificationCodeEmailTemplate;
import com.alrdream.global.security.EmailVerificationCodeStore;
import java.security.SecureRandom;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Phase 23 — 이메일 인증. 이 마이그레이션 이전에 이미 LOCAL로 가입해 미인증 상태로 남은 기존 회원이
 * 마이페이지에서 로그인된 본인 자격으로 이메일 인증을 완료할 때 쓴다(새로 가입하는 회원은
 * {@code SignupVerificationService}가 가입 전에 이미 인증을 강제하므로 이 서비스를 쓸 일이 없다).
 */
@Service
@Transactional(readOnly = true)
public class EmailVerificationService {

	private static final int CODE_TTL_SECONDS = 600; // 10분
	private static final int COOLDOWN_SECONDS = 60;
	private static final int MAX_ATTEMPTS = 5;

	private final MemberRepository memberRepository;
	private final EmailVerificationCodeStore codeStore;
	private final MailService mailService;
	private final SecureRandom random = new SecureRandom();

	public EmailVerificationService(
			MemberRepository memberRepository, EmailVerificationCodeStore codeStore, MailService mailService) {
		this.memberRepository = memberRepository;
		this.codeStore = codeStore;
		this.mailService = mailService;
	}

	/** @return 코드 만료까지 남은 초(프론트가 카운트다운을 표시하는 데 씀) */
	public int requestCode(UUID memberId) {
		Member member = memberRepository.findById(memberId)
				.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));
		if (member.isEmailVerified()) {
			throw new IllegalArgumentException("이미 인증된 계정입니다.");
		}
		if (!codeStore.tryStartCooldown(memberId.toString(), COOLDOWN_SECONDS)) {
			throw new TooManyRequestsException("잠시 후 다시 시도해주세요.", "TOO_MANY_REQUESTS");
		}
		String code = generateCode();
		codeStore.save(memberId.toString(), code, CODE_TTL_SECONDS);
		int ttlMinutes = CODE_TTL_SECONDS / 60;
		String description = "알려드림 계정의 이메일 인증을 위해 아래 코드를 입력해주세요.";
		mailService.sendHtml(
				member.getEmail(),
				"[알려드림] 이메일 인증 코드",
				VerificationCodeEmailTemplate.text(description, code, ttlMinutes),
				VerificationCodeEmailTemplate.html("이메일 인증 코드", description, code, ttlMinutes));
		return CODE_TTL_SECONDS;
	}

	@Transactional
	public void confirmCode(UUID memberId, String code) {
		String key = memberId.toString();
		if (!codeStore.matches(key, code)) {
			codeStore.recordFailedAttempt(key, MAX_ATTEMPTS);
			throw new IllegalArgumentException("코드가 올바르지 않거나 만료되었습니다.");
		}
		Member member = memberRepository.findById(memberId)
				.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));
		member.markEmailVerified();
		codeStore.invalidate(key);
	}

	private String generateCode() {
		return String.format("%06d", random.nextInt(1_000_000));
	}
}
