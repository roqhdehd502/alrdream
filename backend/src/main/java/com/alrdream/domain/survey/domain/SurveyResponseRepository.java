package com.alrdream.domain.survey.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SurveyResponseRepository extends JpaRepository<SurveyResponse, UUID> {

	List<SurveyResponse> findAllByWorkspaceIdOrderBySubmittedAtDesc(UUID workspaceId);

	Optional<SurveyResponse> findByIdAndWorkspaceId(UUID id, UUID workspaceId);
}
