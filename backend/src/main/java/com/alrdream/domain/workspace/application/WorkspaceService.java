package com.alrdream.domain.workspace.application;

import com.alrdream.domain.workspace.domain.Workspace;
import com.alrdream.domain.workspace.domain.WorkspaceRepository;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class WorkspaceService {

	// Querydsl 정렬 매핑(WorkspaceQueryRepositoryImpl)이 실제로 지원하는 필드와 동기화해서 유지한다.
	private static final Set<String> SORTABLE_PROPERTIES = Set.of("name", "createdAt", "updatedAt");

	private final WorkspaceRepository workspaceRepository;

	public WorkspaceService(WorkspaceRepository workspaceRepository) {
		this.workspaceRepository = workspaceRepository;
	}

	@Transactional
	public Workspace create(UUID userId, String name) {
		return workspaceRepository.save(Workspace.create(userId, name));
	}

	public Page<Workspace> list(UUID userId, String keyword, Pageable pageable) {
		// @Repository 빈(WorkspaceQueryRepositoryImpl)에서 IllegalArgumentException을 던지면 Spring의 JPA
		// 예외 변환(PersistenceExceptionTranslationInterceptor)이 InvalidDataAccessApiUsageException으로
		// 감싸버려 GlobalExceptionHandler의 전용 핸들러를 못 타고 500이 된다 — 그래서 검증을 @Service 경계에서 먼저 한다.
		validateSort(pageable.getSort());
		return workspaceRepository.search(userId, keyword, pageable);
	}

	private void validateSort(Sort sort) {
		for (Sort.Order order : sort) {
			if (!SORTABLE_PROPERTIES.contains(order.getProperty())) {
				throw new IllegalArgumentException("지원하지 않는 정렬 필드입니다: " + order.getProperty());
			}
		}
	}

	public Workspace getOwned(UUID workspaceId, UUID userId) {
		return workspaceRepository.findByIdAndUserIdAndDeletedAtIsNull(workspaceId, userId)
				.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 워크스페이스입니다."));
	}

	@Transactional
	public Workspace rename(UUID workspaceId, UUID userId, String name) {
		Workspace workspace = getOwned(workspaceId, userId);
		workspace.rename(name);
		return workspace;
	}

	@Transactional
	public void delete(UUID workspaceId, UUID userId) {
		Workspace workspace = getOwned(workspaceId, userId);
		workspace.markDeleted();
	}
}
