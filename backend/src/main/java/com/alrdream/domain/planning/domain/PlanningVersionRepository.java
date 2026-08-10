package com.alrdream.domain.planning.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlanningVersionRepository extends JpaRepository<PlanningVersion, UUID> {

	Optional<PlanningVersion> findByIdAndWorkspaceIdAndDeletedAtIsNull(UUID id, UUID workspaceId);

	// 내부(AnalysisFeatureOptionResolver 등)에서 "가장 최근 완료된 버전"을 찾는 용도로 여전히 필요해 남겨둔다 —
	// 사용자에게 노출되는 목록 API는 아래 페이징 버전을 쓴다 (Phase 16).
	List<PlanningVersion> findAllByWorkspaceIdAndDeletedAtIsNullOrderByVersionNoDesc(UUID workspaceId);

	Page<PlanningVersion> findAllByWorkspaceIdAndDeletedAtIsNull(UUID workspaceId, Pageable pageable);

	List<PlanningVersion> findAllByIdInAndWorkspaceIdAndDeletedAtIsNull(List<UUID> ids, UUID workspaceId);

	// 버전 번호는 소프트 삭제 후에도 재사용하지 않으므로 deletedAt 여부와 무관하게 최댓값을 본다.
	Optional<PlanningVersion> findFirstByWorkspaceIdOrderByVersionNoDesc(UUID workspaceId);
}
