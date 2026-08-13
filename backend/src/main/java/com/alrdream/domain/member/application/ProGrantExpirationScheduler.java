package com.alrdream.domain.member.application;

import com.alrdream.domain.member.domain.Member;
import com.alrdream.domain.member.domain.MemberPlan;
import com.alrdream.domain.member.domain.MemberRepository;
import com.alrdream.domain.subscription.domain.SubscriptionRepository;
import com.alrdream.domain.subscription.domain.SubscriptionStatus;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Phase 19 — 쿠폰/관리자 지급으로만 Pro인(결제 구독이 뒷받침하지 않는) 사용자의 {@code proExpiresAt}이
 * 지나면 FREE로 되돌린다. 결제 구독이 살아있는(ACTIVE/PAST_DUE) 사용자는 건드리지 않는다 — 그쪽은 결제
 * 웹훅/해지 흐름이 plan을 관리한다(이 스케줄러가 개입하면 이중 관리로 꼬인다). 이 코드베이스의 첫
 * {@code @Scheduled} 잡이라 {@link com.alrdream.BackendApplication}에 {@code @EnableScheduling}을 새로 켰다.
 */
@Component
public class ProGrantExpirationScheduler {

	private static final Logger log = LoggerFactory.getLogger(ProGrantExpirationScheduler.class);

	private final MemberRepository memberRepository;
	private final SubscriptionRepository subscriptionRepository;

	public ProGrantExpirationScheduler(MemberRepository memberRepository, SubscriptionRepository subscriptionRepository) {
		this.memberRepository = memberRepository;
		this.subscriptionRepository = subscriptionRepository;
	}

	@Scheduled(cron = "0 0 * * * *")
	@Transactional
	public void expireProGrants() {
		OffsetDateTime now = OffsetDateTime.now();
		List<Member> candidates = memberRepository.findAllByPlanAndProExpiresAtBefore(MemberPlan.PRO, now);
		int expired = 0;
		for (Member member : candidates) {
			if (hasActiveSubscription(member.getId())) {
				continue;
			}
			member.clearProGrant();
			expired++;
		}
		if (expired > 0) {
			log.info("쿠폰/관리자 지급 Pro 만료 처리: {}건", expired);
		}
	}

	private boolean hasActiveSubscription(UUID userId) {
		return subscriptionRepository.findFirstByUserIdOrderByStartedAtDesc(userId)
				.map(subscription -> subscription.getStatus() == SubscriptionStatus.ACTIVE
						|| subscription.getStatus() == SubscriptionStatus.PAST_DUE)
				.orElse(false);
	}
}
