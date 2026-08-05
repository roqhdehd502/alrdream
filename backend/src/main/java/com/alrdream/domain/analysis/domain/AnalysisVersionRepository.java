package com.alrdream.domain.analysis.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnalysisVersionRepository extends JpaRepository<AnalysisVersion, UUID> {

	Optional<AnalysisVersion> findByIdAndPlanningVersionIdAndDeletedAtIsNull(UUID id, UUID planningVersionId);

	List<AnalysisVersion> findAllByPlanningVersionIdAndDeletedAtIsNullOrderByVersionNoDesc(UUID planningVersionId);

	List<AnalysisVersion> findAllByIdInAndPlanningVersionIdAndDeletedAtIsNull(List<UUID> ids, UUID planningVersionId);

	// 버전 번호는 소프트 삭제 후에도 재사용하지 않으므로 deletedAt 여부와 무관하게 최댓값을 본다.
	Optional<AnalysisVersion> findFirstByPlanningVersionIdOrderByVersionNoDesc(UUID planningVersionId);

	// [02] §5-3 DESIGN 설문의 동적 옵션 — 워크스페이스에 속한 여러 planning_version 중 가장 최근에 완료된 분석을 찾는다.
	List<AnalysisVersion> findAllByPlanningVersionIdInAndStatusAndDeletedAtIsNullOrderByCreatedAtDesc(
			List<UUID> planningVersionIds, AnalysisVersionStatus status);
}
