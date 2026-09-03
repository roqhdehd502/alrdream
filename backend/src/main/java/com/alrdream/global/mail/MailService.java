package com.alrdream.global.mail;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

/**
 * Phase 16 — Gmail SMTP({@code spring.mail.*}) 기반 범용 발송 컴포넌트. 텍스트/HTML 메일을 보내는 로직
 * 자체는 도메인에 무관해 여기 하나로 둔다.
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

	/**
	 * Phase 23 — HTML 본문 메일. {@code text}는 HTML을 지원하지 않는 클라이언트를 위한 대체 텍스트라
	 * html과 같은 정보를 담아야 한다(멀티파트 alternative로 함께 보낸다).
	 */
	public void sendHtml(String to, String subject, String text, String html) {
		MimeMessage message = javaMailSender.createMimeMessage();
		try {
			// text/html alternative(setText(text, html))를 쓰려면 멀티파트 모드로 만들어야 한다 — 그냥
			// false로 두면 "Not in multipart mode" IllegalStateException이 난다.
			MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
			helper.setFrom(fromAddress);
			helper.setTo(to);
			helper.setSubject(subject);
			helper.setText(text, html);
		} catch (MessagingException e) {
			throw new MailPreparationException("HTML 메일을 준비하는 데 실패했습니다.", e);
		}
		javaMailSender.send(message);
	}
}
