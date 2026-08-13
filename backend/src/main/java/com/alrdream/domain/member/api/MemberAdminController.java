package com.alrdream.domain.member.api;

import com.alrdream.domain.member.api.dto.BanMemberRequest;
import com.alrdream.domain.member.api.dto.BulkProGrantRequest;
import com.alrdream.domain.member.api.dto.MemberAdminResponse;
import com.alrdream.domain.member.application.MemberAdminService;
import com.alrdream.domain.member.application.MemberService;
import com.alrdream.global.error.ErrorResponse;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Member (Admin)", description = "사용자 CS 조회/관리 API — Admin 전용 [03] §2-1")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/admin/users")
public class MemberAdminController {

	private final MemberService memberService;
	private final MemberAdminService memberAdminService;

	public MemberAdminController(MemberService memberService, MemberAdminService memberAdminService) {
		this.memberService = memberService;
		this.memberAdminService = memberAdminService;
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

	@Operation(summary = "Pro 일괄 지급/연장", description = "대상이 Free면 신규 지급, 이미 Pro면 보장 기간을 지정한 일수만큼 늘린다.")
	@ApiResponse(responseCode = "200", description = "지급 성공")
	@ApiResponse(responseCode = "400", description = "존재하지 않는 회원이 포함됨",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@PostMapping("/pro-grant")
	public ResponseEntity<Void> bulkGrantPro(@Valid @RequestBody BulkProGrantRequest request) {
		memberAdminService.bulkGrantPro(request.userIds(), request.days());
		return ResponseEntity.ok().build();
	}

	@Operation(summary = "Free로 강제 전환", description = "활성 구독이 있으면 PortOne 결제 예약도 함께 취소한다. 쿠폰 등으로 남은 보장 기간도 무조건 지운다.")
	@ApiResponse(responseCode = "200", description = "전환 성공")
	@PostMapping("/{userId}/downgrade")
	public ResponseEntity<MemberAdminResponse> downgradeToFree(@PathVariable UUID userId) {
		return ResponseEntity.ok(MemberAdminResponse.from(memberAdminService.downgradeToFree(userId)));
	}

	@Operation(summary = "계정 제재", description = "permanent=true면 영구 제재, false면 until까지 일시 제재한다.")
	@ApiResponse(responseCode = "200", description = "제재 성공")
	@ApiResponse(responseCode = "400", description = "일시 제재인데 until이 없거나 과거임")
	@PostMapping("/{userId}/ban")
	public ResponseEntity<MemberAdminResponse> ban(@PathVariable UUID userId, @RequestBody BanMemberRequest request) {
		return ResponseEntity.ok(
				MemberAdminResponse.from(memberAdminService.ban(userId, request.permanent(), request.until())));
	}

	@Operation(summary = "계정 제재 해제")
	@ApiResponse(responseCode = "200", description = "해제 성공")
	@PostMapping("/{userId}/unban")
	public ResponseEntity<MemberAdminResponse> unban(@PathVariable UUID userId) {
		return ResponseEntity.ok(MemberAdminResponse.from(memberAdminService.unban(userId)));
	}
}
