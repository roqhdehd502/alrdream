package com.alrdream.domain.ai.api;

import com.alrdream.domain.ai.api.dto.AiGenerationJobResponse;
import com.alrdream.domain.ai.application.AiGenerationJobService;
import com.alrdream.domain.ai.domain.AiGenerationJob;
import com.alrdream.global.error.ErrorResponse;
import com.alrdream.global.security.MemberPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "AI Generation Job", description = "AI 생성 작업(Job) 상태 폴링 API [03] §4-4")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/ai-generation-jobs")
public class AiGenerationJobController {

	private final AiGenerationJobService aiGenerationJobService;

	public AiGenerationJobController(AiGenerationJobService aiGenerationJobService) {
		this.aiGenerationJobService = aiGenerationJobService;
	}

	@Operation(
			summary = "AI 생성 작업 상태 조회",
			description = "PENDING/PROCESSING/COMPLETED/FAILED 상태를 폴링한다. 본인이 생성한 작업만 조회할 수 있다.")
	@ApiResponse(responseCode = "200", description = "조회 성공")
	@ApiResponse(responseCode = "400", description = "존재하지 않는 작업",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@GetMapping("/{jobId}")
	public ResponseEntity<AiGenerationJobResponse> get(
			@AuthenticationPrincipal MemberPrincipal principal, @PathVariable UUID jobId) {
		AiGenerationJob job = aiGenerationJobService.getOwned(jobId, principal.memberId());
		return ResponseEntity.ok(AiGenerationJobResponse.of(job));
	}
}
