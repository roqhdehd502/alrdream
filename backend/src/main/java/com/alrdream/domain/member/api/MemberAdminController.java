package com.alrdream.domain.member.api;

import com.alrdream.domain.member.api.dto.MemberAdminResponse;
import com.alrdream.domain.member.application.MemberService;
import com.alrdream.global.error.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
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

@Tag(name = "Member (Admin)", description = "사용자 CS 조회 API — Admin 전용 [03] §2-1")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/admin/users")
public class MemberAdminController {

	private final MemberService memberService;

	public MemberAdminController(MemberService memberService) {
		this.memberService = memberService;
	}

	@Operation(summary = "사용자 목록 조회", description = "이메일 부분 일치 검색을 지원한다. CS 대응용 조회 화면 — 상태 확인 위주, 수정/삭제는 지원하지 않는다.")
	@ApiResponse(responseCode = "200", description = "조회 성공")
	@GetMapping
	public ResponseEntity<PagedModel<MemberAdminResponse>> list(
			@Parameter(description = "이메일 검색어 (부분 일치, 대소문자 무시)") @RequestParam(required = false) String keyword,
			@ParameterObject
			@PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
		Page<MemberAdminResponse> page = memberService.search(keyword, pageable).map(MemberAdminResponse::from);
		return ResponseEntity.ok(new PagedModel<>(page));
	}

	@Operation(summary = "사용자 상세 조회")
	@ApiResponse(responseCode = "200", description = "조회 성공")
	@ApiResponse(responseCode = "400", description = "존재하지 않는 사용자",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@GetMapping("/{userId}")
	public ResponseEntity<MemberAdminResponse> get(@PathVariable UUID userId) {
		return ResponseEntity.ok(MemberAdminResponse.from(memberService.getById(userId)));
	}
}
