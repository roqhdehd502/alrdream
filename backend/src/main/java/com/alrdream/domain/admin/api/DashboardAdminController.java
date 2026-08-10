package com.alrdream.domain.admin.api;

import com.alrdream.domain.admin.api.dto.DashboardSummaryResponse;
import com.alrdream.domain.admin.application.DashboardAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Dashboard (Admin)", description = "가입자/생성량/결제 등 운영 지표 요약 — Admin 전용 (Phase 16)")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/admin/dashboard")
public class DashboardAdminController {

	private final DashboardAdminService dashboardAdminService;

	public DashboardAdminController(DashboardAdminService dashboardAdminService) {
		this.dashboardAdminService = dashboardAdminService;
	}

	@Operation(summary = "대시보드 요약 통계", description = "가입자 수(FREE/PRO), 이번 달 AI 생성 건수, 이번 달 결제 성공/실패 건수를 반환한다.")
	@ApiResponse(responseCode = "200", description = "조회 성공")
	@GetMapping("/summary")
	public ResponseEntity<DashboardSummaryResponse> summary() {
		return ResponseEntity.ok(dashboardAdminService.summary());
	}
}
