package com.alrdream.global.mail;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * Phase 16 — Gmail SMTP({@code spring.mail.*}) 기반 범용 발송 컴포넌트. 지금은 비밀번호 재설정 코드
 * 발송에만 쓰이지만, 텍스트 메일을 보내는 로직 자체는 도메인에 무관해 여기 하나로 둔다.
 */
@Component
public class MailService {

	private final JavaMailSender javaMailSender;
	private final String fromAddress;

	public MailService(JavaMailSender javaMailSender, @Value("${spring.mail.username:}") String fromAddress) {
		this.javaMailSender = javaMailSender;
		this.fromAddress = fromAddress;
	}

	public void send(String to, String subject, String text) {
		SimpleMailMessage message = new SimpleMailMessage();
		message.setFrom(fromAddress);
		message.setTo(to);
		message.setSubject(subject);
		message.setText(text);
		javaMailSender.send(message);
	}
}
