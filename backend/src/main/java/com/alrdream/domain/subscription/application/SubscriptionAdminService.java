package com.alrdream.domain.subscription.application;

import com.alrdream.domain.member.domain.Member;
import com.alrdream.domain.member.domain.MemberRepository;
import com.alrdream.domain.subscription.api.dto.SubscriptionAdminResponse;
import com.alrdream.domain.subscription.api.dto.SubscriptionSummaryResponse;
import com.alrdream.domain.subscription.domain.Subscription;
import com.alrdream.domain.subscription.domain.SubscriptionRepository;
import com.alrdream.domain.subscription.domain.SubscriptionStatus;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** [03] §2-1 Admin의 "구독/사용량 대시보드" — 결제 관련 쓰기 로직({@link SubscriptionService})과 분리해 조회만 담당한다. */
@Service
@Transactional(readOnly = true)
public class SubscriptionAdminService {

	private final SubscriptionRepository subscriptionRepository;
	private final MemberRepository memberRepository;

	public SubscriptionAdminService(SubscriptionRepository subscriptionRepository, MemberRepository memberRepository) {
		this.subscriptionRepository = subscriptionRepository;
		this.memberRepository = memberRepository;
	}

	public Page<SubscriptionAdminResponse> list(SubscriptionStatus status, Pageable pageable) {
		Page<Subscription> page = status == null
				? subscriptionRepository.findAll(pageable)
				: subscriptionRepository.findAllByStatus(status, pageable);

		// 목록 화면에 이메일을 함께 보여주기 위해 페이지에 담긴 사용자만 한 번에 조회한다(N+1 방지).
		Map<UUID, String> emailByUserId = memberRepository
				.findAllById(page.getContent().stream().map(Subscription::getUserId).distinct().toList())
				.stream()
				.collect(Collectors.toMap(Member::getId, Member::getEmail));

		return page.map(subscription ->
				SubscriptionAdminResponse.of(subscription, emailByUserId.get(subscription.getUserId())));
	}

	public SubscriptionSummaryResponse summary() {
		return new SubscriptionSummaryResponse(
				subscriptionRepository.countByStatus(SubscriptionStatus.ACTIVE),
				subscriptionRepository.countByStatus(SubscriptionStatus.PAST_DUE),
				subscriptionRepository.countByStatus(SubscriptionStatus.CANCELED));
	}
}
