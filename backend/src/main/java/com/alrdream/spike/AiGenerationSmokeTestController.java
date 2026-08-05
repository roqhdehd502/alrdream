package com.alrdream.spike;

import com.alrdream.domain.ai.api.dto.AiGenerationJobResponse;
import com.alrdream.domain.ai.application.AiGenerationJobService;
import com.alrdream.domain.ai.application.UsageQuotaService;
import com.alrdream.domain.ai.domain.AiGenerationJob;
import com.alrdream.domain.ai.domain.AiTargetType;
import com.alrdream.global.security.MemberPrincipal;
import com.alrdream.infrastructure.ai.AiClient;
import com.alrdream.infrastructure.ai.AiGenerationRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * [04_milestone.md] Phase 06 AI 연동 인프라 스파이크 전용 엔드포인트. Claude Tool Use 기반 구조화 출력 +
 * ai_generation_jobs 비동기 처리 + quota 체크 전체 파이프라인이 실제로 동작하는지 확인하는 용도로,
 * Phase 07(기획 도메인)에서 실제 생성 흐름이 만들어지면 삭제한다.
 */
@Tag(name = "Spike", description = "검증용 임시 엔드포인트 — 해당 기능의 정식 Phase 구현 후 삭제 예정")
@RestController
public class AiGenerationSmokeTestController {

	private static final Logger log = LoggerFactory.getLogger(AiGenerationSmokeTestController.class);

	private static final String SMOKE_TEST_SCHEMA = """
			{
			  "type": "object",
			  "properties": {
			    "one_line_summary": {"type": "string"},
			    "target_customer": {"type": "string"}
			  },
			  "required": ["one_line_summary", "target_customer"]
			}
			""";

	private final UsageQuotaService usageQuotaService;
	private final AiGenerationJobService aiGenerationJobService;
	private final AiClient aiClient;

	public AiGenerationSmokeTestController(
			UsageQuotaService usageQuotaService, AiGenerationJobService aiGenerationJobService, AiClient aiClient) {
		this.usageQuotaService = usageQuotaService;
		this.aiGenerationJobService = aiGenerationJobService;
		this.aiClient = aiClient;
	}

	@Operation(
			summary = "AI 생성 인프라 스모크 테스트",
			description = "quota 체크 → Job 생성(PENDING) → Virtual Thread 비동기로 Claude 호출 → Tool Use 구조화 "
					+ "출력 파싱까지 전체 파이프라인을 실행한다. 반환된 jobId를 GET /api/ai-generation-jobs/{jobId}로 폴링해 결과를 확인한다.")
	@PostMapping("/spike/ai-generation-smoke-test")
	public ResponseEntity<AiGenerationJobResponse> run(@AuthenticationPrincipal MemberPrincipal principal) {
		usageQuotaService.checkAndIncrement(principal.memberId());

		UUID fakeTargetId = UUID.randomUUID();
		AiGenerationJob job = aiGenerationJobService.submit(
				principal.memberId(),
				AiTargetType.PLANNING,
				fakeTargetId,
				() -> {
					AiGenerationRequest request = new AiGenerationRequest(
							"You are a helpful assistant that extracts structured business idea summaries in Korean.",
							"아이디어: 야근이 많은 직장인을 위한 저녁 식사 정기 배달 구독 서비스",
							"emit_summary",
							"아이디어 요약을 구조화된 형태로 반환한다.",
							SMOKE_TEST_SCHEMA);
					String result = aiClient.generateStructuredJson(request);
					log.info("AI 생성 스모크 테스트 결과: {}", result);
				});

		return ResponseEntity.ok(AiGenerationJobResponse.of(job));
	}
}
