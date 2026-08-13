package com.alrdream.domain.member.application;

import com.alrdream.domain.member.domain.Member;
import com.alrdream.domain.member.domain.MemberRepository;
import com.alrdream.domain.subscription.domain.SubscriptionRepository;
import com.alrdream.domain.subscription.domain.SubscriptionStatus;
import com.alrdream.global.security.RefreshTokenStore;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional(readOnly = true)
public class MemberService {

	private final MemberRepository memberRepository;
	private final SubscriptionRepository subscriptionRepository;
	private final RefreshTokenStore refreshTokenStore;

	public MemberService(
			MemberRepository memberRepository,
			SubscriptionRepository subscriptionRepository,
			RefreshTokenStore refreshTokenStore) {
		this.memberRepository = memberRepository;
		this.subscriptionRepository = subscriptionRepository;
		this.refreshTokenStore = refreshTokenStore;
	}

	public Member getById(UUID memberId) {
		return memberRepository.findById(memberId)
				.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));
	}

	/**
	 * Phase 16 — 회원 탈퇴. subscriptions/payment_history/workspaces 등이 FK로 참조해 하드 삭제는 불가능하므로
	 * {@link Member#withdraw()}로 개인정보만 익명화한다. 구독 중(CANCELED가 아닌 상태)이면 막는다 — 아직
	 * 자체 구독 해지 API가 없어(별도 과제) 여기서 자동 해지까지 하면 실제 결제 예약을 PortOne에서 되돌리는
	 * 로직까지 검증 없이 끼워 넣게 되므로, 안전하게 사용자가 먼저 해지하도록 유도한다.
	 */
	@Transactional
	public void withdraw(UUID memberId) {
		Member member = getById(memberId);
		subscriptionRepository.findFirstByUserIdOrderByStartedAtDesc(memberId).ifPresent(subscription -> {
			if (subscription.getStatus() != SubscriptionStatus.CANCELED) {
				throw new IllegalArgumentException("구독 중에는 탈퇴할 수 없습니다. 구독을 먼저 해지해주세요.");
			}
		});
		member.withdraw();
		refreshTokenStore.invalidate(memberId);
	}

	/** Phase 20 — 표시 이름 변경(옵셔널). 빈 값 전달은 {@link Member#changeName}이 "이름 지우기"로 처리한다. */
	@Transactional
	public Member updateName(UUID memberId, String name) {
		Member member = getById(memberId);
		member.changeName(name);
		return member;
	}

	/**
	 * {@link MemberAdminService#downgradeToFree}의 마지막 DB 반영 단계 — 별도 빈의 별도 트랜잭션 메서드로 둬야
	 * {@code MemberAdminService}가 PortOne 호출 단계를 트랜잭션 밖에 둘 수 있다(같은 빈 안에서 {@code this.xxx()}
	 * 자가 호출로는 프록시를 거치지 않아 {@code @Transactional}이 무시되는 문제 — SubscriptionService의 동일
	 * 주석 참고).
	 */
	@Transactional
	public Member clearProGrant(UUID memberId) {
		Member member = getById(memberId);
		member.clearProGrant();
		return member;
	}

	/** [03] §2-1 Admin의 CS 대응용 사용자 조회 — 이메일 부분 일치 검색. */
	public Page<Member> search(String keyword, Pageable pageable) {
		return StringUtils.hasText(keyword)
				? memberRepository.findByEmailContainingIgnoreCase(keyword, pageable)
				: memberRepository.findAll(pageable);
	}
}
