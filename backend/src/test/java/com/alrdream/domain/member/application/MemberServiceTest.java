package com.alrdream.domain.member.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.alrdream.domain.member.domain.Member;
import com.alrdream.domain.member.domain.MemberRepository;
import com.alrdream.domain.subscription.domain.Subscription;
import com.alrdream.domain.subscription.domain.SubscriptionRepository;
import com.alrdream.global.security.RefreshTokenStore;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

	@Mock
	private MemberRepository memberRepository;
	@Mock
	private SubscriptionRepository subscriptionRepository;
	@Mock
	private RefreshTokenStore refreshTokenStore;

	private MemberService memberService;
	private UUID memberId;
	private Member member;

	@BeforeEach
	void setUp() {
		memberService = new MemberService(memberRepository, subscriptionRepository, refreshTokenStore);
		member = Member.createLocal("user@example.com", "hashed");
		memberId = UUID.randomUUID();
		ReflectionTestUtils.setField(member, "id", memberId);
	}

	@Test
	void withdraw_구독_이력이_없으면_탈퇴_가능() {
		when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
		when(subscriptionRepository.findFirstByUserIdOrderByStartedAtDesc(memberId)).thenReturn(Optional.empty());

		memberService.withdraw(memberId);

		assertThat(member.isWithdrawn()).isTrue();
		verify(refreshTokenStore).invalidate(memberId);
	}

	@Test
	void withdraw_해지된_구독만_있으면_탈퇴_가능() {
		Subscription canceled = Subscription.create(memberId, "billing-key");
		canceled.cancel();
		when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
		when(subscriptionRepository.findFirstByUserIdOrderByStartedAtDesc(memberId)).thenReturn(Optional.of(canceled));

		memberService.withdraw(memberId);

		assertThat(member.isWithdrawn()).isTrue();
	}

	@Test
	void withdraw_구독중이면_거부되고_탈퇴되지_않음() {
		Subscription active = Subscription.create(memberId, "billing-key");
		active.activate(java.time.OffsetDateTime.now().plusMonths(1));
		when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
		when(subscriptionRepository.findFirstByUserIdOrderByStartedAtDesc(memberId)).thenReturn(Optional.of(active));

		assertThatThrownBy(() -> memberService.withdraw(memberId)).isInstanceOf(IllegalArgumentException.class);

		assertThat(member.isWithdrawn()).isFalse();
		verify(refreshTokenStore, org.mockito.Mockito.never()).invalidate(memberId);
	}

	@Test
	void updateName_이름을_변경() {
		when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));

		Member result = memberService.updateName(memberId, "홍길동");

		assertThat(result.getName()).isEqualTo("홍길동");
	}

	@Test
	void clearProGrant_Pro_보장_기간이_있어도_무조건_Free로() {
		member.extendProUntil(30);
		when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));

		Member result = memberService.clearProGrant(memberId);

		assertThat(result.getPlan()).isEqualTo(com.alrdream.domain.member.domain.MemberPlan.FREE);
		assertThat(result.getProExpiresAt()).isNull();
	}
}
