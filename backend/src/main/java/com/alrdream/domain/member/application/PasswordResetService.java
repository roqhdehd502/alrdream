package com.alrdream.domain.member.application;

import com.alrdream.domain.member.domain.AuthProvider;
import com.alrdream.domain.member.domain.Member;
import com.alrdream.domain.member.domain.MemberRepository;
import com.alrdream.global.error.TooManyRequestsException;
import com.alrdream.global.mail.MailService;
import com.alrdream.global.security.PasswordResetCodeStore;
import com.alrdream.global.security.RefreshTokenStore;
import java.security.SecureRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Phase 16 — 자체 가입(LOCAL) 계정의 비밀번호 재설정. 이메일로 6자리 코드를 보내고, 코드+새 비밀번호로 확정한다.
 * 이메일 열거 공격을 막기 위해 요청 단계는 계정 존재 여부와 무관하게 항상 같은 응답(204)을 반환한다 —
 * 실제 발송 여부는 내부에서만 갈린다.
 */
@Service
@Transactional(readOnly = true)
public class PasswordResetService {

	private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);

	private static final int CODE_TTL_SECONDS = 600; // 10분
	private static final int COOLDOWN_SECONDS = 60;
	private static final int MAX_ATTEMPTS = 5;

	private final MemberRepository memberRepository;
	private final PasswordEncoder passwordEncoder;
	private final PasswordResetCodeStore codeStore;
	private final MailService mailService;
	private final RefreshTokenStore refreshTokenStore;
	private final SecureRandom random = new SecureRandom();

	public PasswordResetService(
			MemberRepository memberRepository,
			PasswordEncoder passwordEncoder,
			PasswordResetCodeStore codeStore,
			MailService mailService,
			RefreshTokenStore refreshTokenStore) {
		this.memberRepository = memberRepository;
		this.passwordEncoder = passwordEncoder;
		this.codeStore = codeStore;
		this.mailService = mailService;
		this.refreshTokenStore = refreshTokenStore;
	}

	public void requestReset(String email) {
		// 쿨다운은 계정 존재 여부와 무관하게 항상 먼저 체크한다 — 존재할 때만 체크하면 응답 유무 자체가
		// 이메일 존재 여부를 알려주는 사이드 채널이 된다.
		if (!codeStore.tryStartCooldown(email, COOLDOWN_SECONDS)) {
			throw new TooManyRequestsException("잠시 후 다시 시도해주세요.", "TOO_MANY_REQUESTS");
		}
		memberRepository.findByEmail(email)
				.filter(member -> member.getProvider() == AuthProvider.LOCAL)
				.ifPresent(member -> {
					String code = generateCode();
					codeStore.save(email, code, CODE_TTL_SECONDS);
					try {
						mailService.send(
								email,
								"[알려드림] 비밀번호 재설정 코드",
								"비밀번호 재설정 코드는 " + code + " 입니다.\n10분 이내에 입력해주세요.\n"
										+ "본인이 요청하지 않았다면 이 메일을 무시해주세요.");
					} catch (RuntimeException e) {
						// 발송 실패(SMTP 미설정/일시 장애)를 여기서 삼키지 않고 그대로 던지면, 존재하지 않는
						// 이메일(항상 204)과 달리 이 계정만 500이 나 이메일 존재 여부가 노출된다(열거 공격
						// 사이드 채널) — 응답은 항상 동일하게 유지하고 실패는 로그로만 남긴다.
						log.error("비밀번호 재설정 코드 이메일 발송에 실패했습니다: email={}", email, e);
					}
				});
	}

	@Transactional
	public void confirm(String email, String code, String newPassword) {
		if (!codeStore.matches(email, code)) {
			codeStore.recordFailedAttempt(email, MAX_ATTEMPTS);
			throw new IllegalArgumentException("코드가 올바르지 않거나 만료되었습니다.");
		}
		Member member = memberRepository.findByEmail(email)
				.filter(m -> m.getProvider() == AuthProvider.LOCAL)
				.orElseThrow(() -> new IllegalArgumentException("코드가 올바르지 않거나 만료되었습니다."));

		member.changePassword(passwordEncoder.encode(newPassword));
		codeStore.invalidate(email);
		// 재설정 후 기존에 로그인돼 있던 세션(탈취됐을 수도 있는)을 강제로 끊어 재로그인을 유도한다.
		refreshTokenStore.invalidate(member.getId());
	}

	private String generateCode() {
		return String.format("%06d", random.nextInt(1_000_000));
	}
}
