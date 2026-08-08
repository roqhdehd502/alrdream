package com.alrdream.domain.subscription.domain;

import com.alrdream.domain.member.domain.MemberPlan;
import com.alrdream.global.jpa.BaseEntity;
import com.alrdream.global.security.EncryptedStringConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
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
 * [01] 13번, [03] §4-7, §5 {@code subscriptions} — PortOne 빌링키 기반 정기결제 구독. 첫 결제가 실제로
 * 승인됐는지는 웹훅으로만 확정되므로([03] §4-7 "결제 상태는 웹훅으로 동기화"), 생성 직후에는 아직
 * {@code ACTIVE}로 표시하지 않는다. 스키마에 대기 상태(PENDING)가 없어(ACTIVE/PAST_DUE/CANCELED 셋뿐),
 * "아직 결제가 확정되지 않음"을 {@code PAST_DUE}로 표현한다 — {@link #activate}가 최초 {@code Transaction.Paid}
 * 웹훅에서 비로소 {@code ACTIVE}로 전환한다.
 */
@Getter
@Entity
@Table(name = "subscriptions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Subscription extends BaseEntity {

	@Id
	@UuidGenerator
	private UUID id;

	@Column(name = "user_id", nullable = false)
	private UUID userId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private MemberPlan plan;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private SubscriptionStatus status;

	@Convert(converter = EncryptedStringConverter.class)
	@Column(name = "billing_key")
	private String billingKey;

	@Column(name = "next_billing_at")
	private OffsetDateTime nextBillingAt;

	@Column(name = "started_at", nullable = false)
	private OffsetDateTime startedAt;

	@Column(name = "expires_at")
	private OffsetDateTime expiresAt;

	private Subscription(UUID userId, String billingKey) {
		this.userId = userId;
		this.plan = MemberPlan.PRO;
		this.status = SubscriptionStatus.PAST_DUE;
		this.billingKey = billingKey;
		this.startedAt = OffsetDateTime.now();
	}

	public static Subscription create(UUID userId, String billingKey) {
		return new Subscription(userId, billingKey);
	}

	public void scheduleNextBilling(OffsetDateTime nextBillingAt) {
		this.nextBillingAt = nextBillingAt;
	}

	/** {@code Transaction.Paid} 웹훅 수신 시 호출 — 결제가 실제로 승인됐음을 반영한다. */
	public void activate(OffsetDateTime nextBillingAt) {
		this.status = SubscriptionStatus.ACTIVE;
		this.nextBillingAt = nextBillingAt;
	}

	/** {@code Transaction.Failed} 웹훅 수신 시 호출. */
	public void markPastDue() {
		this.status = SubscriptionStatus.PAST_DUE;
	}

	/** 최초 결제 요청 자체가 실패했을 때(카드사 거절 등, 돈이 실제로 오가지 않음) 호출 — 재구독 시도를 막지 않도록 정리한다. */
	public void cancel() {
		this.status = SubscriptionStatus.CANCELED;
	}
}
