package com.alrdream.domain.workspace.api;

import com.alrdream.domain.workspace.api.dto.CreateWorkspaceRequest;
import com.alrdream.domain.workspace.api.dto.UpdateWorkspaceRequest;
import com.alrdream.domain.workspace.api.dto.WorkspaceResponse;
import com.alrdream.domain.workspace.application.WorkspaceService;
import com.alrdream.global.error.ErrorResponse;
import com.alrdream.global.security.MemberPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PagedModel;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Workspace", description = "워크스페이스 생성/조회/수정/삭제 API [01] 2~4번")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/workspaces")
public class WorkspaceController {

	private final WorkspaceService workspaceService;

	public WorkspaceController(WorkspaceService workspaceService) {
		this.workspaceService = workspaceService;
	}

	@Operation(summary = "워크스페이스 생성", description = "이름을 입력받아 새 워크스페이스를 생성한다. [01] 2번의 분기(아이템 있음/고민 중) 선택은 이후 설문 제출 시점에 결정된다.")
	@ApiResponse(responseCode = "200", description = "생성 성공")
	@ApiResponse(responseCode = "400", description = "요청 형식 오류",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@PostMapping
	public ResponseEntity<WorkspaceResponse> create(
			@AuthenticationPrincipal MemberPrincipal principal,
			@Valid @RequestBody CreateWorkspaceRequest request) {
		return ResponseEntity.ok(WorkspaceResponse.from(workspaceService.create(principal.memberId(), request.name())));
	}

	@Operation(
			summary = "워크스페이스 목록 조회",
			description = "현재 회원 소유의 워크스페이스 목록을 페이지 단위로 조회한다 (소프트 삭제된 항목 제외). "
					+ "정렬 가능 필드: name, createdAt, updatedAt (기본값 createdAt,desc).")
	@ApiResponse(responseCode = "200", description = "조회 성공")
	@ApiResponse(responseCode = "400", description = "지원하지 않는 정렬 필드",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@GetMapping
	public ResponseEntity<PagedModel<WorkspaceResponse>> list(
			@AuthenticationPrincipal MemberPrincipal principal,
			@Parameter(description = "워크스페이스 이름 검색어 (부분 일치, 대소문자 무시)") @RequestParam(required = false) String keyword,
			@ParameterObject
			@PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
		Page<WorkspaceResponse> page = workspaceService.list(principal.memberId(), keyword, pageable)
				.map(WorkspaceResponse::from);
		return ResponseEntity.ok(new PagedModel<>(page));
	}

	@Operation(summary = "워크스페이스 상세 조회", description = "워크스페이스 단건을 조회한다. 기획/분석/설계 탭의 세부 데이터는 각 도메인 API에서 별도 조회한다.")
	@ApiResponse(responseCode = "200", description = "조회 성공")
	@ApiResponse(responseCode = "400", description = "존재하지 않거나 소유하지 않은 워크스페이스",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@GetMapping("/{workspaceId}")
	public ResponseEntity<WorkspaceResponse> get(
			@AuthenticationPrincipal MemberPrincipal principal, @PathVariable UUID workspaceId) {
		return ResponseEntity.ok(WorkspaceResponse.from(workspaceService.getOwned(workspaceId, principal.memberId())));
	}

	@Operation(summary = "워크스페이스 이름 수정", description = "설정 탭에서 워크스페이스 이름을 변경한다. [01] 4번")
	@ApiResponse(responseCode = "200", description = "수정 성공")
	@ApiResponse(responseCode = "400", description = "요청 형식 오류 또는 존재하지 않거나 소유하지 않은 워크스페이스",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@PatchMapping("/{workspaceId}")
	public ResponseEntity<WorkspaceResponse> update(
			@AuthenticationPrincipal MemberPrincipal principal,
			@PathVariable UUID workspaceId,
			@Valid @RequestBody UpdateWorkspaceRequest request) {
		return ResponseEntity.ok(WorkspaceResponse.from(
				workspaceService.rename(workspaceId, principal.memberId(), request.name())));
	}

	@Operation(summary = "워크스페이스 삭제", description = "설정 탭에서 워크스페이스를 삭제한다 (소프트 삭제). [01] 4번")
	@ApiResponse(responseCode = "204", description = "삭제 성공")
	@ApiResponse(responseCode = "400", description = "존재하지 않거나 소유하지 않은 워크스페이스",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@DeleteMapping("/{workspaceId}")
	public ResponseEntity<Void> delete(
			@AuthenticationPrincipal MemberPrincipal principal, @PathVariable UUID workspaceId) {
		workspaceService.delete(workspaceId, principal.memberId());
		return ResponseEntity.noContent().build();
	}
}
