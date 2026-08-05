package com.alrdream.domain.analysis.api;

import com.alrdream.domain.ai.api.dto.AiGenerationJobResponse;
import com.alrdream.domain.ai.domain.AiGenerationJob;
import com.alrdream.domain.analysis.api.dto.AnalysisVersionDetail;
import com.alrdream.domain.analysis.api.dto.AnalysisVersionSummary;
import com.alrdream.domain.analysis.api.dto.DeleteAnalysisVersionsRequest;
import com.alrdream.domain.analysis.application.AnalysisVersionService;
import com.alrdream.domain.document.api.dto.DocumentResponse;
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

@Tag(name = "Analysis", description = "분석(Analysis) 도메인 — 기획안 본문 기반 AI 생성, 설문 없음 [01] 7,8,9번")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/workspaces/{workspaceId}/planning-versions/{planningVersionId}/analysis-versions")
public class AnalysisVersionController {

	private final AnalysisVersionService analysisVersionService;

	public AnalysisVersionController(AnalysisVersionService analysisVersionService) {
		this.analysisVersionService = analysisVersionService;
	}

	@Operation(
			summary = "분석 생성/수정",
			description = "완료된 기획안 버전의 본문을 입력으로 다음 버전의 분석 생성을 시작한다. 별도 설문이 없어 [01] 7번(최초 "
					+ "생성)과 8번(\"수정\")이 입력 차이 없이 이 엔드포인트를 그대로 다시 호출하는 방식이다 — 매번 새 버전이 만들어진다. "
					+ "즉시 Job 정보를 반환하고, 실제 생성은 비동기로 처리된다 — GET /api/ai-generation-jobs/{jobId}로 폴링한다.")
	@ApiResponse(responseCode = "200", description = "Job 생성 성공 (분석은 아직 GENERATING)")
	@ApiResponse(responseCode = "400", description = "워크스페이스/기획안이 없거나 기획안이 아직 완료되지 않음",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@ApiResponse(responseCode = "429", description = "FREE 티어 월별 생성 횟수 초과",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@PostMapping
	public ResponseEntity<AiGenerationJobResponse> create(
			@AuthenticationPrincipal MemberPrincipal principal,
			@PathVariable UUID workspaceId,
			@PathVariable UUID planningVersionId) {
		AiGenerationJob job = analysisVersionService.create(workspaceId, planningVersionId, principal.memberId());
		return ResponseEntity.ok(AiGenerationJobResponse.of(job));
	}

	@Operation(summary = "분석 버전 목록 조회", description = "삭제되지 않은 버전을 최신순으로 조회한다 (content 미포함).")
	@ApiResponse(responseCode = "200", description = "조회 성공")
	@GetMapping
	public ResponseEntity<List<AnalysisVersionSummary>> list(
			@AuthenticationPrincipal MemberPrincipal principal,
			@PathVariable UUID workspaceId,
			@PathVariable UUID planningVersionId) {
		List<AnalysisVersionSummary> versions =
				analysisVersionService.list(workspaceId, planningVersionId, principal.memberId()).stream()
						.map(AnalysisVersionSummary::of)
						.toList();
		return ResponseEntity.ok(versions);
	}

	@Operation(summary = "분석 버전 상세 조회", description = "GENERATING/FAILED면 content가 null이다.")
	@ApiResponse(responseCode = "200", description = "조회 성공")
	@ApiResponse(responseCode = "400", description = "존재하지 않는 분석",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@GetMapping("/{analysisVersionId}")
	public ResponseEntity<AnalysisVersionDetail> get(
			@AuthenticationPrincipal MemberPrincipal principal,
			@PathVariable UUID workspaceId,
			@PathVariable UUID planningVersionId,
			@PathVariable UUID analysisVersionId) {
		return ResponseEntity.ok(AnalysisVersionDetail.of(
				analysisVersionService.getOwned(analysisVersionId, planningVersionId, workspaceId, principal.memberId())));
	}

	@Operation(
			summary = "분석 PDF 다운로드",
			description = "[03] §4-6 — 완료된 분석을 PDF로 렌더링해 Supabase Storage에 업로드하고 서명 URL을 반환한다. "
					+ "이미 생성된 적이 있으면(content는 불변) 다시 렌더링하지 않고 서명 URL만 새로 발급한다.")
	@ApiResponse(responseCode = "200", description = "발급 성공")
	@ApiResponse(responseCode = "400", description = "존재하지 않는 분석이거나 아직 생성이 완료되지 않음",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@PostMapping("/{analysisVersionId}/pdf")
	public ResponseEntity<DocumentResponse> generatePdf(
			@AuthenticationPrincipal MemberPrincipal principal,
			@PathVariable UUID workspaceId,
			@PathVariable UUID planningVersionId,
			@PathVariable UUID analysisVersionId) {
		return ResponseEntity.ok(analysisVersionService.generatePdf(
				analysisVersionId, planningVersionId, workspaceId, principal.memberId()));
	}

	@Operation(summary = "분석 버전 다중 삭제", description = "[01] 9번 — 소프트 삭제. 지정한 ID 중 하나라도 존재하지 않으면 전체가 실패한다.")
	@ApiResponse(responseCode = "204", description = "삭제 성공")
	@ApiResponse(responseCode = "400", description = "존재하지 않는 기획안/분석이 포함됨",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@DeleteMapping
	public ResponseEntity<Void> delete(
			@AuthenticationPrincipal MemberPrincipal principal,
			@PathVariable UUID workspaceId,
			@PathVariable UUID planningVersionId,
			@Valid @RequestBody DeleteAnalysisVersionsRequest request) {
		analysisVersionService.deleteAll(
				workspaceId, planningVersionId, principal.memberId(), request.analysisVersionIds());
		return ResponseEntity.noContent().build();
	}
}
