package com.alrdream.domain.document.domain;

import com.alrdream.domain.ai.domain.AiTargetType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRepository extends JpaRepository<Document, UUID> {

	Optional<Document> findFirstBySourceTypeAndSourceIdOrderByGeneratedAtDesc(AiTargetType sourceType, UUID sourceId);
}
