package com.alrdream.domain.survey.api;

import com.alrdream.domain.survey.api.dto.SubmitSurveyResponseRequest;
import com.alrdream.domain.survey.api.dto.SurveyDefinitionResponse;
import com.alrdream.domain.survey.api.dto.SurveyResponseDetail;
import com.alrdream.domain.survey.api.dto.SurveyResponseSummary;
import com.alrdream.domain.survey.application.SurveyDefinitionService;
import com.alrdream.domain.survey.application.SurveyResponseService;
import com.alrdream.domain.survey.domain.SurveyDefinition;
import com.alrdream.domain.survey.domain.SurveyKey;
import com.alrdream.domain.survey.domain.SurveyResponse;
import com.alrdream.domain.workspace.application.WorkspaceService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Survey", description = "워크스페이스 기준 설문 조회/응답 제출 API [02] §2, §4")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/workspaces/{workspaceId}")
public class SurveyController {

	private final WorkspaceService workspaceService;
	private final SurveyDefinitionService surveyDefinitionService;
	private final SurveyResponseService surveyResponseService;

	public SurveyController(
			WorkspaceService workspaceService,
			SurveyDefinitionService surveyDefinitionService,
			SurveyResponseService surveyResponseService) {
		this.workspaceService = workspaceService;
		this.surveyDefinitionService = surveyDefinitionService;
		this.surveyResponseService = surveyResponseService;
	}

	@Operation(
			summary = "설문 스키마 조회",
			description = "해당 설문 종류의 최신 발행 버전을 반환한다. DESIGN은 [02] §5-3에 따라 core_feature_priority 문항의 "
					+ "options를 이 워크스페이스의 분석 산출물 기준으로 동적 주입한다(분석 도메인이 아직 없어 현재는 항상 빈 배열).")
	@ApiResponse(responseCode = "200", description = "조회 성공")
	@ApiResponse(responseCode = "400", description = "워크스페이스가 없거나 발행된 설문이 없음",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@GetMapping("/surveys/{surveyKey}")
	public ResponseEntity<SurveyDefinitionResponse> getSurvey(
			@AuthenticationPrincipal MemberPrincipal principal,
			@PathVariable UUID workspaceId,
			@PathVariable SurveyKey surveyKey) {
		workspaceService.getOwned(workspaceId, principal.memberId());
		SurveyDefinition definition = surveyDefinitionService.getLatestDefinition(surveyKey);
		return ResponseEntity.ok(SurveyDefinitionResponse.of(
				definition.getId(),
				definition.getSurveyKey(),
				definition.getVersion(),
				definition.getTitle(),
				surveyDefinitionService.getResolvedSchema(surveyKey, workspaceId),
				definition.getCreatedAt()));
	}

	@Operation(
			summary = "설문 응답 제출",
			description = "항상 최신 발행 버전 기준으로 검증한다. 문항 타입 일치, 필수 응답 여부, allowUnknown, 선택형 문항의 "
					+ "옵션 값 유효성을 검증한다. 제출된 응답은 이후 수정할 수 없다(불변) — [01] 5/11번의 \"수정\"은 새 응답을 다시 제출하는 방식이다.")
	@ApiResponse(responseCode = "200", description = "제출 성공")
	@ApiResponse(responseCode = "400", description = "워크스페이스가 없거나, 발행된 설문이 없거나, 답변 검증 실패",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@PostMapping("/survey-responses")
	public ResponseEntity<SurveyResponseDetail> submit(
			@AuthenticationPrincipal MemberPrincipal principal,
			@PathVariable UUID workspaceId,
			@Valid @RequestBody SubmitSurveyResponseRequest request) {
		workspaceService.getOwned(workspaceId, principal.memberId());
		SurveyResponse response = surveyResponseService.submit(workspaceId, request);
		return ResponseEntity.ok(toDetail(response));
	}

	@Operation(summary = "설문 응답 목록 조회", description = "해당 워크스페이스에 제출된 모든 설문 응답을 최신순으로 조회한다.")
	@ApiResponse(responseCode = "200", description = "조회 성공")
	@ApiResponse(responseCode = "400", description = "워크스페이스가 없음",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@GetMapping("/survey-responses")
	public ResponseEntity<List<SurveyResponseSummary>> list(
			@AuthenticationPrincipal MemberPrincipal principal, @PathVariable UUID workspaceId) {
		workspaceService.getOwned(workspaceId, principal.memberId());
		List<SurveyResponseSummary> responses = surveyResponseService.list(workspaceId).stream()
				.map(r -> new SurveyResponseSummary(r.getId(), r.getSurveyDefinitionId(), r.getSubmittedAt()))
				.toList();
		return ResponseEntity.ok(responses);
	}

	@Operation(summary = "설문 응답 상세 조회", description = "복호화된 답변을 포함해 조회한다.")
	@ApiResponse(responseCode = "200", description = "조회 성공")
	@ApiResponse(responseCode = "400", description = "존재하지 않는 워크스페이스 또는 응답",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@GetMapping("/survey-responses/{responseId}")
	public ResponseEntity<SurveyResponseDetail> get(
			@AuthenticationPrincipal MemberPrincipal principal,
			@PathVariable UUID workspaceId,
			@PathVariable UUID responseId) {
		workspaceService.getOwned(workspaceId, principal.memberId());
		return ResponseEntity.ok(toDetail(surveyResponseService.getOwned(responseId, workspaceId)));
	}

	private SurveyResponseDetail toDetail(SurveyResponse response) {
		return new SurveyResponseDetail(
				response.getId(),
				response.getSurveyDefinitionId(),
				response.getSubmittedAt(),
				surveyResponseService.parseAnswers(response));
	}
}
