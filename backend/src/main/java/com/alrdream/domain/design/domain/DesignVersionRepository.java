package com.alrdream.domain.design.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DesignVersionRepository extends JpaRepository<DesignVersion, UUID> {

	Optional<DesignVersion> findByIdAndAnalysisVersionIdAndDeletedAtIsNull(UUID id, UUID analysisVersionId);

	// [03] §5 — 자신 또는 상위(분석/기획)가 소프트 삭제돼도 조회는 허용해야 하는 읽기 경로용(deletedAt 무시).
	Optional<DesignVersion> findByIdAndAnalysisVersionId(UUID id, UUID analysisVersionId);

	List<DesignVersion> findAllByAnalysisVersionIdAndDeletedAtIsNullOrderByVersionNoDesc(UUID analysisVersionId);

	Page<DesignVersion> findAllByAnalysisVersionIdAndDeletedAtIsNull(UUID analysisVersionId, Pageable pageable);

	List<DesignVersion> findAllByIdInAndAnalysisVersionIdAndDeletedAtIsNull(List<UUID> ids, UUID analysisVersionId);

	// 버전 번호는 소프트 삭제 후에도 재사용하지 않으므로 deletedAt 여부와 무관하게 최댓값을 본다.
	Optional<DesignVersion> findFirstByAnalysisVersionIdOrderByVersionNoDesc(UUID analysisVersionId);
}
