package com.alrdream.domain.ai.api;

import com.alrdream.domain.ai.api.dto.UsageQuotaResponse;
import com.alrdream.domain.ai.application.UsageQuotaService;
import com.alrdream.global.security.MemberPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Usage Quota", description = "내 AI 생성 사용량 조회 [01] 13번")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/usage-quota")
public class UsageQuotaController {

	private final UsageQuotaService usageQuotaService;

	public UsageQuotaController(UsageQuotaService usageQuotaService) {
		this.usageQuotaService = usageQuotaService;
	}

	@Operation(
			summary = "내 이번 달 AI 생성 사용량 조회",
			description = "429(생성 횟수 초과)를 맞기 전에 잔여 횟수를 미리 확인할 수 있게 한다. PRO 플랜은 무제한이라 "
					+ "limitCount는 참고용일 뿐 실제로 적용되지 않는다.")
	@ApiResponse(responseCode = "200", description = "조회 성공")
	@GetMapping("/me")
	public ResponseEntity<UsageQuotaResponse> getCurrent(@AuthenticationPrincipal MemberPrincipal principal) {
		return ResponseEntity.ok(UsageQuotaResponse.of(usageQuotaService.getCurrent(principal.memberId())));
	}
}
