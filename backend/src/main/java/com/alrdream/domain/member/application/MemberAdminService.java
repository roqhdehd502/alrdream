package com.alrdream.domain.member.application;

import com.alrdream.domain.member.domain.Member;
import com.alrdream.domain.member.domain.MemberRepository;
import com.alrdream.domain.member.domain.MemberRole;
import com.alrdream.domain.subscription.application.SubscriptionService;
import com.alrdream.domain.subscription.domain.SubscriptionRepository;
import com.alrdream.domain.subscription.domain.SubscriptionStatus;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * [03] §2-1 Admin의 회원 관리 액션(CS 도구) — 다수 Pro 일괄 지급/연장, 강제 Free 전환, 제재. 조회 전용인
 * {@link MemberService}와 분리해 쓰기 동작만 담당한다(SubscriptionService/SubscriptionAdminService가
 * 나뉜 것과 동일한 패턴).
 */
@Service
@Transactional(readOnly = true)
public class MemberAdminService {

	private final MemberRepository memberRepository;
	private final SubscriptionRepository subscriptionRepository;
	private final SubscriptionService subscriptionService;

	public MemberAdminService(
			MemberRepository memberRepository,
			SubscriptionRepository subscriptionRepository,
			SubscriptionService subscriptionService) {
		this.memberRepository = memberRepository;
		this.subscriptionRepository = subscriptionRepository;
		this.subscriptionService = subscriptionService;
	}

	/** [PlanningVersionService#deleteAll]류의 "전체 조회 → 개수 검증 → 없는 id 있으면 통째로 400" 패턴. */
	@Transactional
	public void bulkGrantPro(List<UUID> userIds, int days) {
		List<Member> members = memberRepository.findAllById(userIds);
		if (members.size() != userIds.size()) {
			throw new IllegalArgumentException("존재하지 않는 회원이 포함되어 있습니다.");
		}
		members.forEach(member -> member.extendProUntil(days));
	}

	/**
	 * 활성 구독이 있으면 {@link SubscriptionService#cancelActiveSubscription}로 PortOne 예약까지 함께
	 * 취소한 뒤(그냥 plan만 바꾸면 결제는 계속되는데 앱에서는 Free 취급되는 불일치가 생긴다), 쿠폰 등으로
	 * 남아있는 보장 기간까지 포함해 무조건 Free로 확정한다.
	 */
	@Transactional
	public Member downgradeToFree(UUID userId) {
		subscriptionRepository.findFirstByUserIdOrderByStartedAtDesc(userId)
				.filter(subscription -> subscription.getStatus() != SubscriptionStatus.CANCELED)
				.ifPresent(subscription -> subscriptionService.cancelActiveSubscription(userId));

		Member member = getById(userId);
		member.clearProGrant();
		return member;
	}

	/**
	 * ADMIN 계정은 대상에서 제외한다 — 제재는 이미 발급된 토큰까지 즉시 차단하므로(JwtAuthenticationFilter),
	 * 실수로(또는 잘못된 대상으로) 관리자 계정을 정지시키면 앱 안에서 되돌릴 방법 없이 관리자 콘솔 전체가
	 * 잠기는 사고가 난다(직접 겪고 확인함 — 라이브 검증 중 테스트 관리자 계정을 스스로 영구 정지했더니 그
	 * 즉시 같은 계정의 정지 해제 요청마저 403으로 거부됐다). DB에 직접 접근하지 않고는 복구할 수 없어,
	 * 애초에 ADMIN 대상 제재 자체를 막는다.
	 */
	@Transactional
	public Member ban(UUID userId, boolean permanent, OffsetDateTime until) {
		Member member = getById(userId);
		if (member.getRole() == MemberRole.ADMIN) {
			throw new IllegalArgumentException("관리자 계정은 이 화면에서 제재할 수 없습니다.");
		}
		if (permanent) {
			member.banPermanently();
		} else {
			if (until == null || !until.isAfter(OffsetDateTime.now())) {
				throw new IllegalArgumentException("일시 제재 해제 시각은 현재보다 미래여야 합니다.");
			}
			member.banTemporarily(until);
		}
		return member;
	}

	@Transactional
	public Member unban(UUID userId) {
		Member member = getById(userId);
		member.unban();
		return member;
	}

	private Member getById(UUID userId) {
		return memberRepository.findById(userId)
				.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));
	}
}
