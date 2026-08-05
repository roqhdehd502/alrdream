package com.alrdream.domain.survey.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SurveyDefinitionRepository extends JpaRepository<SurveyDefinition, UUID> {

	Optional<SurveyDefinition> findFirstBySurveyKeyOrderByVersionDesc(SurveyKey surveyKey);

	List<SurveyDefinition> findAllBySurveyKeyOrderByVersionDesc(SurveyKey surveyKey);

	List<SurveyDefinition> findAllByOrderBySurveyKeyAscVersionDesc();
}
