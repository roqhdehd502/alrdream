package com.alrdream.domain.planning.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlanningVersionRepository extends JpaRepository<PlanningVersion, UUID> {

	Optional<PlanningVersion> findByIdAndWorkspaceIdAndDeletedAtIsNull(UUID id, UUID workspaceId);

	List<PlanningVersion> findAllByWorkspaceIdAndDeletedAtIsNullOrderByVersionNoDesc(UUID workspaceId);

	List<PlanningVersion> findAllByIdInAndWorkspaceIdAndDeletedAtIsNull(List<UUID> ids, UUID workspaceId);

	// 버전 번호는 소프트 삭제 후에도 재사용하지 않으므로 deletedAt 여부와 무관하게 최댓값을 본다.
	Optional<PlanningVersion> findFirstByWorkspaceIdOrderByVersionNoDesc(UUID workspaceId);
}
