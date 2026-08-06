package com.alrdream.domain.subscription.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

/**
 * [03] §4-7, §5 {@code payment_history} — 웹훅으로 확정된 결제 시도 1건. {@code payment_id}가 UNIQUE라
 * 웹훅 멱등 처리의 기준 키다({@link com.alrdream.domain.subscription.application.PortOneWebhookService}).
 * {@code updated_at}이 없어 {@link com.alrdream.global.jpa.BaseEntity}는 쓰지 않는다
 * ({@code com.alrdream.domain.document.domain.Document}와 동일한 이유).
 */
@Getter
@Entity
@Table(name = "payment_history")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentHistory {

	@Id
	@UuidGenerator
	private UUID id;

	@Column(name = "subscription_id", nullable = false)
	private UUID subscriptionId;

	@Column(name = "payment_id", nullable = false, unique = true)
	private String paymentId;

	@Column(nullable = false)
	private long amount;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private PaymentStatus status;

	@Column(name = "paid_at")
	private OffsetDateTime paidAt;

	@Column(name = "created_at", nullable = false)
	private OffsetDateTime createdAt;

	private PaymentHistory(UUID subscriptionId, String paymentId, long amount, PaymentStatus status, OffsetDateTime paidAt) {
		this.subscriptionId = subscriptionId;
		this.paymentId = paymentId;
		this.amount = amount;
		this.status = status;
		this.paidAt = paidAt;
		this.createdAt = OffsetDateTime.now();
	}

	public static PaymentHistory create(
			UUID subscriptionId, String paymentId, long amount, PaymentStatus status, OffsetDateTime paidAt) {
		return new PaymentHistory(subscriptionId, paymentId, amount, status, paidAt);
	}
}
