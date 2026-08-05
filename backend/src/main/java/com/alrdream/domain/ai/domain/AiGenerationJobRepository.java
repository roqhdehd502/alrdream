package com.alrdream.domain.ai.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiGenerationJobRepository extends JpaRepository<AiGenerationJob, UUID> {

	Optional<AiGenerationJob> findByIdAndUserId(UUID id, UUID userId);
}
