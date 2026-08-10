package com.alrdream.domain.admin.application;

import com.alrdream.domain.admin.api.dto.DashboardSummaryResponse;
import com.alrdream.domain.ai.domain.AiGenerationJobRepository;
import com.alrdream.domain.member.domain.MemberPlan;
import com.alrdream.domain.member.domain.MemberRepository;
import com.alrdream.domain.subscription.domain.PaymentHistoryRepository;
import com.alrdream.domain.subscription.domain.PaymentStatus;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Phase 16 — Admin 대시보드가 구독 현황(SubscriptionAdminService)만 보여주고 가입자/생성량/결제 추이 같은
 * 운영 지표가 없던 것을 보완한다. 여러 도메인의 집계라 특정 도메인 소속이 아닌 이 패키지에 둔다.
 */
@Service
@Transactional(readOnly = true)
public class DashboardAdminService {

	private final MemberRepository memberRepository;
	private final AiGenerationJobRepository aiGenerationJobRepository;
	private final PaymentHistoryRepository paymentHistoryRepository;

	public DashboardAdminService(
			MemberRepository memberRepository,
			AiGenerationJobRepository aiGenerationJobRepository,
			PaymentHistoryRepository paymentHistoryRepository) {
		this.memberRepository = memberRepository;
		this.aiGenerationJobRepository = aiGenerationJobRepository;
		this.paymentHistoryRepository = paymentHistoryRepository;
	}

	public DashboardSummaryResponse summary() {
		OffsetDateTime startOfMonth = YearMonth.now().atDay(1).atStartOfDay(ZoneId.systemDefault()).toOffsetDateTime();
		return new DashboardSummaryResponse(
				memberRepository.count(),
				memberRepository.countByPlan(MemberPlan.FREE),
				memberRepository.countByPlan(MemberPlan.PRO),
				aiGenerationJobRepository.countByCreatedAtAfter(startOfMonth),
				paymentHistoryRepository.countByStatusAndCreatedAtAfter(PaymentStatus.PAID, startOfMonth),
				paymentHistoryRepository.countByStatusAndCreatedAtAfter(PaymentStatus.FAILED, startOfMonth));
	}
}
