package com.alrdream.domain.survey.api;

import com.alrdream.domain.survey.api.dto.PublishSurveyDefinitionRequest;
import com.alrdream.domain.survey.api.dto.SurveyDefinitionResponse;
import com.alrdream.domain.survey.application.SurveyDefinitionService;
import com.alrdream.domain.survey.domain.SurveyDefinition;
import com.alrdream.domain.survey.domain.SurveyKey;
import com.alrdream.global.error.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "SurveyDefinition (Admin)", description = "설문 정의 발행/조회 API — Admin 전용 [02] §3")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/admin/survey-definitions")
public class SurveyDefinitionAdminController {

	private final SurveyDefinitionService surveyDefinitionService;

	public SurveyDefinitionAdminController(SurveyDefinitionService surveyDefinitionService) {
		this.surveyDefinitionService = surveyDefinitionService;
	}

	@Operation(
			summary = "설문 정의 발행",
			description = "새 버전을 발행한다. 버전 번호는 클라이언트가 지정하지 않고 해당 surveyKey의 최신 버전 + 1로 서버가 계산한다. "
					+ "기존 버전은 절대 수정되지 않는다(불변) — 문항을 바꾸려면 새 버전을 발행한다.")
	@ApiResponse(responseCode = "200", description = "발행 성공")
	@ApiResponse(responseCode = "400", description = "요청 형식 오류",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@PostMapping
	public ResponseEntity<SurveyDefinitionResponse> publish(@Valid @RequestBody PublishSurveyDefinitionRequest request) {
		SurveyDefinition definition = surveyDefinitionService.publish(request);
		return ResponseEntity.ok(toResponse(definition));
	}

	@Operation(summary = "설문 정의 목록 조회", description = "surveyKey로 필터링할 수 있다. 생략하면 전체 설문 종류의 모든 버전을 반환한다.")
	@ApiResponse(responseCode = "200", description = "조회 성공")
	@GetMapping
	public ResponseEntity<List<SurveyDefinitionResponse>> list(
			@Parameter(description = "설문 종류 필터") @RequestParam(required = false) SurveyKey surveyKey) {
		List<SurveyDefinitionResponse> responses = surveyDefinitionService.list(surveyKey).stream()
				.map(this::toResponse)
				.toList();
		return ResponseEntity.ok(responses);
	}

	@Operation(summary = "설문 정의 상세 조회")
	@ApiResponse(responseCode = "200", description = "조회 성공")
	@ApiResponse(responseCode = "400", description = "존재하지 않는 설문 정의",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@GetMapping("/{id}")
	public ResponseEntity<SurveyDefinitionResponse> get(@PathVariable UUID id) {
		return ResponseEntity.ok(toResponse(surveyDefinitionService.getById(id)));
	}

	private SurveyDefinitionResponse toResponse(SurveyDefinition definition) {
		return SurveyDefinitionResponse.of(
				definition.getId(),
				definition.getSurveyKey(),
				definition.getVersion(),
				definition.getTitle(),
				surveyDefinitionService.parseSchema(definition),
				definition.getCreatedAt());
	}
}
