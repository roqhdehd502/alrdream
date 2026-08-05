package com.alrdream.domain.design.api;

import com.alrdream.domain.ai.api.dto.AiGenerationJobResponse;
import com.alrdream.domain.ai.domain.AiGenerationJob;
import com.alrdream.domain.design.api.dto.CreateDesignVersionRequest;
import com.alrdream.domain.design.api.dto.DeleteDesignVersionsRequest;
import com.alrdream.domain.design.api.dto.DesignVersionDetail;
import com.alrdream.domain.design.api.dto.DesignVersionSummary;
import com.alrdream.domain.design.application.DesignVersionService;
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

@Tag(name = "Design", description = "설계(Design) 도메인 — 분석 결과 + DESIGN 설문 기반 AI 생성 [01] 10,11번")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping(
		"/api/workspaces/{workspaceId}/planning-versions/{planningVersionId}"
				+ "/analysis-versions/{analysisVersionId}/design-versions")
public class DesignVersionController {

	private final DesignVersionService designVersionService;

	public DesignVersionController(DesignVersionService designVersionService) {
		this.designVersionService = designVersionService;
	}

	@Operation(
			summary = "설계 생성/수정",
			description = "완료된 분석 버전 + DESIGN 설문 응답을 입력으로 다음 버전의 설계 생성을 시작한다. [01] 10번(최초 생성)과 "
					+ "11번(\"수정\") 모두 이 엔드포인트를 쓴다 — \"수정\"은 편집된 답변으로 새 DESIGN 설문 응답을 먼저 제출한 뒤"
					+ "(SurveyController), 그 응답으로 다시 이 엔드포인트를 호출하는 방식이다([02] §7, 기존 응답은 불변). "
					+ "즉시 Job 정보를 반환하고, 실제 생성은 비동기로 처리된다 — GET /api/ai-generation-jobs/{jobId}로 폴링한다.")
	@ApiResponse(responseCode = "200", description = "Job 생성 성공 (설계는 아직 GENERATING)")
	@ApiResponse(responseCode = "400", description = "워크스페이스/분석/설문 응답이 없거나, 분석이 아직 완료되지 않았거나, DESIGN 설문 응답이 아님",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@ApiResponse(responseCode = "429", description = "FREE 티어 월별 생성 횟수 초과",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@PostMapping
	public ResponseEntity<AiGenerationJobResponse> create(
			@AuthenticationPrincipal MemberPrincipal principal,
			@PathVariable UUID workspaceId,
			@PathVariable UUID planningVersionId,
			@PathVariable UUID analysisVersionId,
			@Valid @RequestBody CreateDesignVersionRequest request) {
		AiGenerationJob job = designVersionService.create(
				workspaceId, planningVersionId, analysisVersionId, principal.memberId(), request.surveyResponseId());
		return ResponseEntity.ok(AiGenerationJobResponse.of(job));
	}

	@Operation(summary = "설계 버전 목록 조회", description = "삭제되지 않은 버전을 최신순으로 조회한다 (content 미포함).")
	@ApiResponse(responseCode = "200", description = "조회 성공")
	@GetMapping
	public ResponseEntity<List<DesignVersionSummary>> list(
			@AuthenticationPrincipal MemberPrincipal principal,
			@PathVariable UUID workspaceId,
			@PathVariable UUID planningVersionId,
			@PathVariable UUID analysisVersionId) {
		List<DesignVersionSummary> versions = designVersionService
				.list(workspaceId, planningVersionId, analysisVersionId, principal.memberId())
				.stream()
				.map(DesignVersionSummary::of)
				.toList();
		return ResponseEntity.ok(versions);
	}

	@Operation(summary = "설계 버전 상세 조회", description = "GENERATING/FAILED면 content가 null이다.")
	@ApiResponse(responseCode = "200", description = "조회 성공")
	@ApiResponse(responseCode = "400", description = "존재하지 않는 설계",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@GetMapping("/{designVersionId}")
	public ResponseEntity<DesignVersionDetail> get(
			@AuthenticationPrincipal MemberPrincipal principal,
			@PathVariable UUID workspaceId,
			@PathVariable UUID planningVersionId,
			@PathVariable UUID analysisVersionId,
			@PathVariable UUID designVersionId) {
		return ResponseEntity.ok(DesignVersionDetail.of(designVersionService.getOwned(
				designVersionId, analysisVersionId, planningVersionId, workspaceId, principal.memberId())));
	}

	@Operation(summary = "설계 버전 다중 삭제", description = "소프트 삭제. 지정한 ID 중 하나라도 존재하지 않으면 전체가 실패한다.")
	@ApiResponse(responseCode = "204", description = "삭제 성공")
	@ApiResponse(responseCode = "400", description = "존재하지 않는 분석/설계가 포함됨",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@DeleteMapping
	public ResponseEntity<Void> delete(
			@AuthenticationPrincipal MemberPrincipal principal,
			@PathVariable UUID workspaceId,
			@PathVariable UUID planningVersionId,
			@PathVariable UUID analysisVersionId,
			@Valid @RequestBody DeleteDesignVersionsRequest request) {
		designVersionService.deleteAll(
				workspaceId, planningVersionId, analysisVersionId, principal.memberId(), request.designVersionIds());
		return ResponseEntity.noContent().build();
	}
}
