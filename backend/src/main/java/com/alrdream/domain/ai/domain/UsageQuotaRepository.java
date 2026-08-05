package com.alrdream.domain.ai.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsageQuotaRepository extends JpaRepository<UsageQuota, UUID> {

	Optional<UsageQuota> findByUserIdAndPeriod(UUID userId, String period);
}
