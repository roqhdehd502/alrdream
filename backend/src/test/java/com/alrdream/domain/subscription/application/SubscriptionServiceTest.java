package com.alrdream.domain.subscription.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.alrdream.domain.member.domain.Member;
import com.alrdream.domain.member.domain.MemberPlan;
import com.alrdream.domain.member.domain.MemberRepository;
import com.alrdream.domain.subscription.domain.PaymentHistoryRepository;
import com.alrdream.domain.subscription.domain.Subscription;
import com.alrdream.domain.subscription.domain.SubscriptionRepository;
import com.alrdream.domain.subscription.domain.SubscriptionStatus;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * {@link SubscriptionService} 단위 테스트 — PortOne SDK를 실제로 호출하는 경로(chargeFirstPayment/
 * scheduleNextPayment/revokeNextPaymentSchedule)는 외부 API 왕복이 필요해 이번 phase 스코프에서는 실제
 * PortOne 샌드박스로 라이브 검증했다(Phase 21 참고). 여기서는 DB 로직만으로 완결되는 부분 — 특히 Phase 21에서
 * revoke/finalize로 트랜잭션 경계를 분리한 뒤 그 각 조각이 올바르게 동작하는지를 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

	@Mock
	private SubscriptionRepository subscriptionRepository;
	@Mock
	private PaymentHistoryRepository paymentHistoryRepository;
	@Mock
	private MemberRepository memberRepository;

	private SubscriptionService subscriptionService;
	private UUID userId;

	@BeforeEach
	void setUp() {
		subscriptionService = new SubscriptionService(
				subscriptionRepository, paymentHistoryRepository, memberRepository, null, null, "test-channel-key");
		userId = UUID.randomUUID();
	}

	private Subscription subscriptionWith(UUID id) {
		Subscription subscription = Subscription.create(userId, "billing-key");
		ReflectionTestUtils.setField(subscription, "id", id);
		return subscription;
	}

	@Test
	void createPendingSubscription_구독_이력이_없으면_생성_가능() {
		when(subscriptionRepository.findFirstByUserIdOrderByStartedAtDesc(userId)).thenReturn(Optional.empty());
		when(subscriptionRepository.save(org.mockito.ArgumentMatchers.any()))
				.thenAnswer(inv -> inv.getArgument(0));

		Subscription result = subscriptionService.createPendingSubscription(userId, "billing-key");

		assertThat(result.getStatus()).isEqualTo(SubscriptionStatus.PAST_DUE);
	}

	@Test
	void createPendingSubscription_해지된_구독만_있으면_재구독_가능() {
		Subscription canceled = subscriptionWith(UUID.randomUUID());
		canceled.cancel();
		when(subscriptionRepository.findFirstByUserIdOrderByStartedAtDesc(userId)).thenReturn(Optional.of(canceled));
		when(subscriptionRepository.save(org.mockito.ArgumentMatchers.any()))
				.thenAnswer(inv -> inv.getArgument(0));

		subscriptionService.createPendingSubscription(userId, "billing-key"); // 예외 없이 통과하면 성공
	}

	@Test
	void createPendingSubscription_이미_구독중이면_거부() {
		Subscription active = subscriptionWith(UUID.randomUUID());
		active.activate(OffsetDateTime.now().plusMonths(1));
		when(subscriptionRepository.findFirstByUserIdOrderByStartedAtDesc(userId)).thenReturn(Optional.of(active));

		assertThatThrownBy(() -> subscriptionService.createPendingSubscription(userId, "billing-key"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("이미 구독 중");
	}

	@Test
	void cancelSubscription_존재하면_CANCELED로_전환() {
		Subscription subscription = subscriptionWith(UUID.randomUUID());
		when(subscriptionRepository.findById(subscription.getId())).thenReturn(Optional.of(subscription));

		subscriptionService.cancelSubscription(subscription.getId());

		assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.CANCELED);
	}

	@Test
	void cancelSubscription_존재하지_않으면_조용히_무시() {
		UUID missingId = UUID.randomUUID();
		when(subscriptionRepository.findById(missingId)).thenReturn(Optional.empty());

		subscriptionService.cancelSubscription(missingId); // 예외 없이 통과하면 성공
	}

	@Test
	void finalizeCancelation_구독을_CANCELED로_확정하고_회원_plan을_동기화() {
		Subscription subscription = subscriptionWith(UUID.randomUUID());
		Member member = Member.createLocal("user@example.com", "hashed");
		when(subscriptionRepository.findById(subscription.getId())).thenReturn(Optional.of(subscription));
		when(memberRepository.findById(userId)).thenReturn(Optional.of(member));

		subscriptionService.finalizeCancelation(subscription.getId(), userId);

		assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.CANCELED);
		assertThat(member.getPlan()).isEqualTo(MemberPlan.FREE); // 쿠폰 보장 기간이 없어 FREE로
	}

	@Test
	void finalizeCancelation_쿠폰_보장_기간이_남아있으면_회원_plan은_유지() {
		Subscription subscription = subscriptionWith(UUID.randomUUID());
		Member member = Member.createLocal("user@example.com", "hashed");
		member.extendProUntil(30); // 쿠폰으로 30일 보장된 상태
		when(subscriptionRepository.findById(subscription.getId())).thenReturn(Optional.of(subscription));
		when(memberRepository.findById(userId)).thenReturn(Optional.of(member));

		subscriptionService.finalizeCancelation(subscription.getId(), userId);

		assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.CANCELED); // 구독 자체는 해지되지만
		assertThat(member.getPlan()).isEqualTo(MemberPlan.PRO); // 쿠폰 보장 기간 동안은 Pro 유지
	}

	@Test
	void finalizeCancelation_구독을_찾을_수_없으면_예외() {
		UUID missingId = UUID.randomUUID();
		when(subscriptionRepository.findById(missingId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> subscriptionService.finalizeCancelation(missingId, userId))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void getCurrent_구독_이력이_없으면_예외() {
		when(subscriptionRepository.findFirstByUserIdOrderByStartedAtDesc(userId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> subscriptionService.getCurrent(userId)).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void getPaymentHistory_구독_이력이_없으면_빈_목록을_즉시_반환() {
		when(subscriptionRepository.findAllByUserIdOrderByStartedAtDesc(userId)).thenReturn(List.of());

		List<?> result = subscriptionService.getPaymentHistory(userId);

		assertThat(result).isEmpty();
		org.mockito.Mockito.verifyNoInteractions(paymentHistoryRepository); // 빈 목록이면 조회 자체를 생략
	}

	@Test
	void rootMessage_원인_예외의_메시지를_우선_사용() {
		RuntimeException cause = new RuntimeException("실제 원인 메시지");
		RuntimeException wrapper = new RuntimeException("래핑 메시지", cause);

		assertThat(SubscriptionService.rootMessage(wrapper)).isEqualTo("실제 원인 메시지");
	}

	@Test
	void rootMessage_원인_예외에_메시지가_없으면_클래스명으로_폴백() {
		RuntimeException cause = new IllegalStateException(); // 메시지 없음
		RuntimeException wrapper = new RuntimeException("래핑 메시지", cause);

		assertThat(SubscriptionService.rootMessage(wrapper)).isEqualTo("IllegalStateException");
	}

	@Test
	void rootMessage_원인이_없으면_자기_자신을_사용() {
		RuntimeException e = new RuntimeException("단독 예외");

		assertThat(SubscriptionService.rootMessage(e)).isEqualTo("단독 예외");
	}
}
