package com.alrdream.domain.ai.domain;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiGenerationJobRepository extends JpaRepository<AiGenerationJob, UUID> {

	Optional<AiGenerationJob> findByIdAndUserId(UUID id, UUID userId);

	/** Phase 16 — Admin 대시보드 통계("이번 달 생성 건수"). FREE/PRO 구분 없이 실제 생성 시도 전체를 센다. */
	long countByCreatedAtAfter(OffsetDateTime after);
}
