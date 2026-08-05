package com.alrdream.domain.workspace.domain;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface WorkspaceQueryRepository {

	Page<Workspace> search(UUID userId, String keyword, Pageable pageable);
}
