package com.alrdream.domain.subscription.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.alrdream.domain.member.domain.MemberPlan;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SubscriptionTest {

	@Test
	void create_PAST_DUE_상태로_시작() {
		Subscription subscription = Subscription.create(UUID.randomUUID(), "billing-key");

		assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.PAST_DUE);
		assertThat(subscription.getPlan()).isEqualTo(MemberPlan.PRO);
	}

	@Test
	void activate_결제_승인_웹훅으로_ACTIVE_전환() {
		Subscription subscription = Subscription.create(UUID.randomUUID(), "billing-key");
		OffsetDateTime nextBillingAt = OffsetDateTime.now().plusMonths(1);

		subscription.activate(nextBillingAt);

		assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
		assertThat(subscription.getNextBillingAt()).isEqualTo(nextBillingAt);
	}

	@Test
	void markPastDue_결제_실패_웹훅으로_PAST_DUE_전환() {
		Subscription subscription = Subscription.create(UUID.randomUUID(), "billing-key");
		subscription.activate(OffsetDateTime.now().plusMonths(1));

		subscription.markPastDue();

		assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.PAST_DUE);
	}

	@Test
	void cancel_다음_결제_예약_정보를_모두_비움() {
		Subscription subscription = Subscription.create(UUID.randomUUID(), "billing-key");
		subscription.scheduleNextBilling(OffsetDateTime.now().plusMonths(1), "schedule-1");

		subscription.cancel();

		assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.CANCELED);
		assertThat(subscription.getNextBillingAt()).isNull();
		assertThat(subscription.getNextPaymentScheduleId()).isNull();
		assertThat(subscription.getExpiresAt()).isNotNull();
	}
}
