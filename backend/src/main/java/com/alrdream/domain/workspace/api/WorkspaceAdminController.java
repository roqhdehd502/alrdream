package com.alrdream.domain.workspace.api;

import com.alrdream.domain.workspace.api.dto.WorkspaceResponse;
import com.alrdream.domain.workspace.application.WorkspaceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PagedModel;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Workspace (Admin)", description = "사용자별 워크스페이스 CS 조회 API — Admin 전용 [03] §2-1")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/admin/users/{userId}/workspaces")
public class WorkspaceAdminController {

	private final WorkspaceService workspaceService;

	public WorkspaceAdminController(WorkspaceService workspaceService) {
		this.workspaceService = workspaceService;
	}

	@Operation(summary = "사용자별 워크스페이스 목록 조회", description = "소프트 삭제된 항목은 제외한다. CS 대응용 — 수정/삭제는 지원하지 않는다.")
	@ApiResponse(responseCode = "200", description = "조회 성공")
	@GetMapping
	public ResponseEntity<PagedModel<WorkspaceResponse>> list(
			@PathVariable UUID userId,
			@Parameter(description = "워크스페이스 이름 검색어 (부분 일치, 대소문자 무시)") @RequestParam(required = false) String keyword,
			@ParameterObject
			@PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
		Page<WorkspaceResponse> page = workspaceService.list(userId, keyword, pageable).map(WorkspaceResponse::from);
		return ResponseEntity.ok(new PagedModel<>(page));
	}
}
