package com.alrdream.domain.ai.application;

import com.alrdream.domain.ai.domain.UsageQuota;
import com.alrdream.domain.ai.domain.UsageQuotaRepository;
import com.alrdream.domain.member.domain.Member;
import com.alrdream.domain.member.domain.MemberPlan;
import com.alrdream.domain.member.domain.MemberRepository;
import com.alrdream.global.error.TooManyRequestsException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.YearMonth;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** [01] 13번, [03] §4-4 — AI 생성 Job을 만들기 전에 호출해 FREE 플랜의 월별 생성 횟수 한도를 검사/차감한다. */
@Service
public class UsageQuotaService {

	private final UsageQuotaRepository usageQuotaRepository;
	private final MemberRepository memberRepository;
	private final int freeTierMonthlyLimit;

	@PersistenceContext
	private EntityManager entityManager;

	public UsageQuotaService(
			UsageQuotaRepository usageQuotaRepository,
			MemberRepository memberRepository,
			@Value("${app.ai.free-tier-monthly-limit}") int freeTierMonthlyLimit) {
		this.usageQuotaRepository = usageQuotaRepository;
		this.memberRepository = memberRepository;
		this.freeTierMonthlyLimit = freeTierMonthlyLimit;
	}

	/** PRO 플랜은 무제한([01] 13번 "이후" BM)이라 통과시키고, FREE는 이번 달 quota row를 조회/생성 후 한도를 검사하고 즉시 1 차감한다. */
	@Transactional
	public void checkAndIncrement(UUID userId) {
		Member member = memberRepository.findById(userId)
				.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));
		if (member.getPlan() == MemberPlan.PRO) {
			return;
		}

		String period = YearMonth.now().toString();

		// 동시 요청이 같은 사용자+기간의 quota row를 동시에 최초 생성하려다 UNIQUE(user_id, period) 제약에
		// 걸리거나, 한도 검사를 동시에 통과해 실제 한도보다 더 많이 생성되는 것을 막기 위해 직렬화한다
		// (survey_definitions 발행 동시성 문제와 동일한 원인 — [04_milestone.md] Phase 05 참고).
		entityManager.createNativeQuery("SELECT pg_advisory_xact_lock(hashtext(:key)::bigint)")
				.setParameter("key", "usage_quota:" + userId + ":" + period)
				.getSingleResult();

		UsageQuota quota = usageQuotaRepository.findByUserIdAndPeriod(userId, period)
				.orElseGet(() -> usageQuotaRepository.save(UsageQuota.create(userId, period, freeTierMonthlyLimit)));

		if (quota.isExceeded()) {
			throw new TooManyRequestsException("이번 달 무료 생성 횟수(" + quota.getLimitCount() + "회)를 모두 사용했습니다.");
		}
		quota.increment();
	}
}
