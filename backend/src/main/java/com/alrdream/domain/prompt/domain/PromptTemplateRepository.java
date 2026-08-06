package com.alrdream.domain.prompt.domain;

import com.alrdream.domain.ai.domain.AiTargetType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PromptTemplateRepository extends JpaRepository<PromptTemplate, UUID> {

	Optional<PromptTemplate> findFirstByPromptTypeOrderByVersionDesc(AiTargetType promptType);

	List<PromptTemplate> findAllByPromptTypeOrderByVersionDesc(AiTargetType promptType);

	List<PromptTemplate> findAllByOrderByPromptTypeAscVersionDesc();
}
