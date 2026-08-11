package com.alrdream.domain.subscription.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

	Optional<Subscription> findFirstByUserIdOrderByStartedAtDesc(UUID userId);

	/** 결제 내역 조회용 — 해지 후 재구독으로 여러 행이 쌓일 수 있어 이 사용자의 전체 구독 이력을 본다. */
	List<Subscription> findAllByUserIdOrderByStartedAtDesc(UUID userId);

	/** [03] §2-1 Admin의 "구독 현황" 목록. */
	Page<Subscription> findAllByStatus(SubscriptionStatus status, Pageable pageable);

	long countByStatus(SubscriptionStatus status);
}
