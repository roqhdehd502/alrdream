package com.alrdream.domain.subscription.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionPricingRepository extends JpaRepository<SubscriptionPricing, UUID> {

	List<SubscriptionPricing> findAllByOrderByCreatedAtAsc();
}
