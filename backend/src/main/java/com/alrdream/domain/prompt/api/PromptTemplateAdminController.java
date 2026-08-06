package com.alrdream.domain.prompt.api;

import com.alrdream.domain.ai.domain.AiTargetType;
import com.alrdream.domain.prompt.api.dto.PromptTemplateResponse;
import com.alrdream.domain.prompt.api.dto.PublishPromptTemplateRequest;
import com.alrdream.domain.prompt.application.PromptTemplateService;
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

@Tag(name = "PromptTemplate (Admin)", description = "AI 프롬프트 템플릿 발행/조회 API — Admin 전용 [03] §2-1")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/admin/prompt-templates")
public class PromptTemplateAdminController {

	private final PromptTemplateService promptTemplateService;

	public PromptTemplateAdminController(PromptTemplateService promptTemplateService) {
		this.promptTemplateService = promptTemplateService;
	}

	@Operation(
			summary = "프롬프트 템플릿 발행",
			description = "새 버전을 발행한다. 버전 번호는 클라이언트가 지정하지 않고 해당 promptType의 최신 버전 + 1로 서버가 계산한다. "
					+ "기존 버전은 절대 수정되지 않는다(불변) — 내용을 바꾸려면 새 버전을 발행한다. 새로 발행된 버전은 즉시 해당 타입의 "
					+ "다음 생성부터 실제로 사용된다(활성 버전 = 최신 버전).")
	@ApiResponse(responseCode = "200", description = "발행 성공")
	@ApiResponse(responseCode = "400", description = "요청 형식 오류",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@PostMapping
	public ResponseEntity<PromptTemplateResponse> publish(@Valid @RequestBody PublishPromptTemplateRequest request) {
		return ResponseEntity.ok(PromptTemplateResponse.from(promptTemplateService.publish(request)));
	}

	@Operation(summary = "프롬프트 템플릿 목록 조회", description = "promptType으로 필터링할 수 있다. 생략하면 전체 타입의 모든 버전을 반환한다.")
	@ApiResponse(responseCode = "200", description = "조회 성공")
	@GetMapping
	public ResponseEntity<List<PromptTemplateResponse>> list(
			@Parameter(description = "프롬프트 대상 필터") @RequestParam(required = false) AiTargetType promptType) {
		List<PromptTemplateResponse> responses = promptTemplateService.list(promptType).stream()
				.map(PromptTemplateResponse::from)
				.toList();
		return ResponseEntity.ok(responses);
	}

	@Operation(summary = "프롬프트 템플릿 상세 조회")
	@ApiResponse(responseCode = "200", description = "조회 성공")
	@ApiResponse(responseCode = "400", description = "존재하지 않는 프롬프트 템플릿",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@GetMapping("/{id}")
	public ResponseEntity<PromptTemplateResponse> get(@PathVariable UUID id) {
		return ResponseEntity.ok(PromptTemplateResponse.from(promptTemplateService.getById(id)));
	}
}
