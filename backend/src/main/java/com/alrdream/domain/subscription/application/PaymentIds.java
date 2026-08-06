package com.alrdream.domain.subscription.application;

import java.util.UUID;

/**
 * PortOne에 보내는 {@code paymentId}는 고객사(우리)가 채번한다. {@code subscriptionId}를 구분자 "_"로 감싸
 * 인코딩해두면, 웹훅 수신 시(subscriptionId는 UUID라 "_"를 포함하지 않음) 별도 조회 없이 문자열만으로
 * 어느 구독에 대한 결제인지 되짚을 수 있다({@link PortOneWebhookService}).
 */
final class PaymentIds {

	private static final String PREFIX = "sub";
	private static final String DELIMITER = "_";

	private PaymentIds() {
	}

	static String generate(UUID subscriptionId) {
		return PREFIX + DELIMITER + subscriptionId + DELIMITER + UUID.randomUUID();
	}

	static UUID parseSubscriptionId(String paymentId) {
		String[] parts = paymentId.split(DELIMITER);
		if (parts.length != 3 || !parts[0].equals(PREFIX)) {
			throw new IllegalArgumentException("알 수 없는 paymentId 형식입니다: " + paymentId);
		}
		return UUID.fromString(parts[1]);
	}
}
