package com.alrdream.domain.subscription.application;

import com.alrdream.domain.member.application.MemberService;
import com.alrdream.domain.member.domain.Member;
import com.alrdream.domain.member.domain.MemberRepository;
import com.alrdream.domain.subscription.api.dto.PaymentAdminResponse;
import com.alrdream.domain.subscription.domain.PaymentHistory;
import com.alrdream.domain.subscription.domain.PaymentHistoryRepository;
import com.alrdream.domain.subscription.domain.PaymentStatus;
import com.alrdream.domain.subscription.domain.Subscription;
import com.alrdream.domain.subscription.domain.SubscriptionRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** [03] §2-1 Admin의 "결제 관리" — 개별 결제 이력 조회(대시보드의 이번 달 성공/실패 집계와 별개). */
@Service
@Transactional(readOnly = true)
public class PaymentAdminService {

	private final PaymentHistoryRepository paymentHistoryRepository;
	private final SubscriptionRepository subscriptionRepository;
	private final MemberRepository memberRepository;
	private final MemberService memberService;

	public PaymentAdminService(
			PaymentHistoryRepository paymentHistoryRepository,
			SubscriptionRepository subscriptionRepository,
			MemberRepository memberRepository,
			MemberService memberService) {
		this.paymentHistoryRepository = paymentHistoryRepository;
		this.subscriptionRepository = subscriptionRepository;
		this.memberRepository = memberRepository;
		this.memberService = memberService;
	}

	public Page<PaymentAdminResponse> list(PaymentStatus status, Pageable pageable) {
		Page<PaymentHistory> page = status == null
				? paymentHistoryRepository.findAll(pageable)
				: paymentHistoryRepository.findAllByStatus(status, pageable);

		Map<UUID, UUID> userIdBySubscriptionId = subscriptionRepository
				.findAllById(page.getContent().stream().map(PaymentHistory::getSubscriptionId).distinct().toList())
				.stream()
				.collect(Collectors.toMap(Subscription::getId, Subscription::getUserId));

		Map<UUID, String> emailByUserId = memberRepository
				.findAllById(userIdBySubscriptionId.values().stream().distinct().toList())
				.stream()
				.collect(Collectors.toMap(Member::getId, Member::getEmail));

		return page.map(payment -> {
			UUID userId = userIdBySubscriptionId.get(payment.getSubscriptionId());
			return PaymentAdminResponse.of(payment, userId, userId == null ? null : emailByUserId.get(userId));
		});
	}

	public Page<PaymentAdminResponse> listForUser(UUID userId, Pageable pageable) {
		Member member = memberService.getById(userId);
		List<UUID> subscriptionIds = subscriptionRepository.findAllByUserIdOrderByStartedAtDesc(userId).stream()
				.map(Subscription::getId)
				.toList();
		if (subscriptionIds.isEmpty()) {
			return Page.empty(pageable);
		}
		return paymentHistoryRepository.findAllBySubscriptionIdIn(subscriptionIds, pageable)
				.map(payment -> PaymentAdminResponse.of(payment, userId, member.getEmail()));
	}
}
