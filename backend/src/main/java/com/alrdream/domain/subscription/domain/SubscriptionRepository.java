package com.alrdream.domain.subscription.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

	Optional<Subscription> findFirstByUserIdOrderByStartedAtDesc(UUID userId);

	/** [03] §2-1 Admin의 "구독 현황" 목록. */
	Page<Subscription> findAllByStatus(SubscriptionStatus status, Pageable pageable);

	long countByStatus(SubscriptionStatus status);
}
