package com.alrdream.domain.planning.api;

import com.alrdream.domain.ai.api.dto.AiGenerationJobResponse;
import com.alrdream.domain.ai.domain.AiGenerationJob;
import com.alrdream.domain.document.api.dto.DocumentResponse;
import com.alrdream.domain.planning.api.dto.CreatePlanningVersionRequest;
import com.alrdream.domain.planning.api.dto.DeletePlanningVersionsRequest;
import com.alrdream.domain.planning.api.dto.PlanningVersionDetail;
import com.alrdream.domain.planning.api.dto.PlanningVersionSummary;
import com.alrdream.domain.planning.application.PlanningVersionService;
import com.alrdream.global.error.ErrorResponse;
import com.alrdream.global.security.MemberPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Planning", description = "기획(Planning) 도메인 — 설문 응답 기반 AI 생성 [01] 2,5,6번")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/workspaces/{workspaceId}/planning-versions")
public class PlanningVersionController {

	private final PlanningVersionService planningVersionService;

	public PlanningVersionController(PlanningVersionService planningVersionService) {
		this.planningVersionService = planningVersionService;
	}

	@Operation(
			summary = "기획안 생성/수정",
			description = "설문 응답 하나를 입력으로 다음 버전의 기획안 생성을 시작한다. [01] 2번(최초 생성)과 5번(\"수정\") 모두 "
					+ "이 엔드포인트를 쓴다 — \"수정\"은 편집된 답변으로 새 설문 응답을 먼저 제출한 뒤(SurveyController), 그 "
					+ "응답으로 다시 이 엔드포인트를 호출하는 방식이다([02] §7, 기존 응답은 불변). 즉시 Job 정보를 반환하고, "
					+ "실제 생성은 비동기로 처리된다 — GET /api/ai-generation-jobs/{jobId}로 폴링한다.")
	@ApiResponse(responseCode = "200", description = "Job 생성 성공 (기획안은 아직 GENERATING)")
	@ApiResponse(responseCode = "400", description = "워크스페이스/설문 응답이 없거나 DESIGN 설문 응답을 사용함",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@ApiResponse(responseCode = "429", description = "FREE 티어 월별 생성 횟수 초과",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@PostMapping
	public ResponseEntity<AiGenerationJobResponse> create(
			@AuthenticationPrincipal MemberPrincipal principal,
			@PathVariable UUID workspaceId,
			@Valid @RequestBody CreatePlanningVersionRequest request) {
		AiGenerationJob job = planningVersionService.create(workspaceId, principal.memberId(), request.surveyResponseId());
		return ResponseEntity.ok(AiGenerationJobResponse.of(job));
	}

	@Operation(summary = "기획안 버전 목록 조회", description = "삭제되지 않은 버전을 최신순으로 조회한다 (content 미포함).")
	@ApiResponse(responseCode = "200", description = "조회 성공")
	@GetMapping
	public ResponseEntity<List<PlanningVersionSummary>> list(
			@AuthenticationPrincipal MemberPrincipal principal, @PathVariable UUID workspaceId) {
		List<PlanningVersionSummary> versions = planningVersionService.list(workspaceId, principal.memberId()).stream()
				.map(PlanningVersionSummary::of)
				.toList();
		return ResponseEntity.ok(versions);
	}

	@Operation(summary = "기획안 버전 상세 조회", description = "GENERATING/FAILED면 content가 null이다.")
	@ApiResponse(responseCode = "200", description = "조회 성공")
	@ApiResponse(responseCode = "400", description = "존재하지 않는 기획안",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@GetMapping("/{versionId}")
	public ResponseEntity<PlanningVersionDetail> get(
			@AuthenticationPrincipal MemberPrincipal principal,
			@PathVariable UUID workspaceId,
			@PathVariable UUID versionId) {
		return ResponseEntity.ok(
				PlanningVersionDetail.of(planningVersionService.getOwned(versionId, workspaceId, principal.memberId())));
	}

	@Operation(
			summary = "기획안 PDF 다운로드",
			description = "[03] §4-6 — 완료된 기획안을 PDF로 렌더링해 Supabase Storage에 업로드하고 서명 URL을 반환한다. "
					+ "이미 생성된 적이 있으면(content는 불변) 다시 렌더링하지 않고 서명 URL만 새로 발급한다.")
	@ApiResponse(responseCode = "200", description = "발급 성공")
	@ApiResponse(responseCode = "400", description = "존재하지 않는 기획안이거나 아직 생성이 완료되지 않음",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@PostMapping("/{versionId}/pdf")
	public ResponseEntity<DocumentResponse> generatePdf(
			@AuthenticationPrincipal MemberPrincipal principal,
			@PathVariable UUID workspaceId,
			@PathVariable UUID versionId) {
		return ResponseEntity.ok(planningVersionService.generatePdf(versionId, workspaceId, principal.memberId()));
	}

	@Operation(summary = "기획안 버전 다중 삭제", description = "[01] 6번 — 소프트 삭제. 지정한 ID 중 하나라도 존재하지 않으면 전체가 실패한다.")
	@ApiResponse(responseCode = "204", description = "삭제 성공")
	@ApiResponse(responseCode = "400", description = "워크스페이스가 없거나 존재하지 않는 기획안이 포함됨",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@DeleteMapping
	public ResponseEntity<Void> delete(
			@AuthenticationPrincipal MemberPrincipal principal,
			@PathVariable UUID workspaceId,
			@Valid @RequestBody DeletePlanningVersionsRequest request) {
		planningVersionService.deleteAll(workspaceId, principal.memberId(), request.versionIds());
		return ResponseEntity.noContent().build();
	}
}
