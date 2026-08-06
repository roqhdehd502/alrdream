package com.alrdream.infrastructure.payment;

import io.portone.sdk.server.payment.PaymentClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * [03] §4-7 — PortOne V2 JVM Server SDK 클라이언트 빈. {@code WebhookVerifier}는 여기서 빈으로 만들지 않는다 —
 * 그 생성자가 secret을 즉시 base64 디코딩해 검증하므로, 아직 웹훅 secret을 발급받지 못한 상태(빈 값)에서는
 * 애플리케이션 부팅 자체가 실패한다. {@code EncryptedStringConverter}와 같은 이유로, secret 유효성 검사를
 * 실제 웹훅 수신 시점까지 미루기 위해 {@code PortOneWebhookService}에서 매 호출마다 생성한다.
 */
@Configuration
public class PortOneClientConfig {

	@Bean(destroyMethod = "close")
	public PaymentClient paymentClient(
			@Value("${app.portone.api-secret}") String apiSecret,
			@Value("${app.portone.api-base}") String apiBase,
			@Value("${app.portone.store-id}") String storeId) {
		return new PaymentClient(apiSecret, apiBase, storeId);
	}
}
