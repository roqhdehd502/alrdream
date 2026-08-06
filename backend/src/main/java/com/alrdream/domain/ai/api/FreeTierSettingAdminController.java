package com.alrdream.domain.ai.api;

import com.alrdream.domain.ai.api.dto.FreeTierLimitResponse;
import com.alrdream.domain.ai.api.dto.UpdateFreeTierLimitRequest;
import com.alrdream.domain.ai.application.FreeTierSettingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "FreeTierSetting (Admin)", description = "FREE 플랜 월별 생성 횟수 한도 조회/조정 — Admin 전용 [01] 13번, [03] §2-1")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/admin/settings/free-tier-limit")
public class FreeTierSettingAdminController {

	private final FreeTierSettingService freeTierSettingService;

	public FreeTierSettingAdminController(FreeTierSettingService freeTierSettingService) {
		this.freeTierSettingService = freeTierSettingService;
	}

	@Operation(summary = "FREE 플랜 월별 생성 횟수 한도 조회")
	@ApiResponse(responseCode = "200", description = "조회 성공")
	@GetMapping
	public ResponseEntity<FreeTierLimitResponse> get() {
		return ResponseEntity.ok(FreeTierLimitResponse.from(freeTierSettingService.get()));
	}

	@Operation(
			summary = "FREE 플랜 월별 생성 횟수 한도 변경",
			description = "이후 새로 생성되는 quota 행(다음 달 첫 생성 시점 등)부터 적용된다. 이번 달 이미 생성된 usage_quotas 행의 "
					+ "limit_count는 소급 변경되지 않는다.")
	@ApiResponse(responseCode = "200", description = "변경 성공")
	@PutMapping
	public ResponseEntity<FreeTierLimitResponse> update(@Valid @RequestBody UpdateFreeTierLimitRequest request) {
		return ResponseEntity.ok(FreeTierLimitResponse.from(freeTierSettingService.updateMonthlyLimit(request.monthlyLimit())));
	}
}
