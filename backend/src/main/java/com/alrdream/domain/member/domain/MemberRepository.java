package com.alrdream.domain.member.domain;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, UUID> {

	Optional<Member> findByEmail(String email);

	Optional<Member> findByProviderAndProviderId(AuthProvider provider, String providerId);

	boolean existsByEmail(String email);

	/** [03] §2-1 Admin의 사용자 CS 조회. */
	Page<Member> findByEmailContainingIgnoreCase(String keyword, Pageable pageable);

	/** Phase 16 — Admin 대시보드 통계. */
	long countByPlan(MemberPlan plan);

	/** Phase 19 — ProGrantExpirationScheduler가 쿠폰/관리자 지급 Pro 만료 후보를 찾을 때 쓴다. */
	List<Member> findAllByPlanAndProExpiresAtBefore(MemberPlan plan, OffsetDateTime time);
}
