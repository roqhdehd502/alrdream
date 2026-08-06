package com.alrdream.domain.subscription.api;

import com.alrdream.domain.subscription.api.dto.SubscriptionAdminResponse;
import com.alrdream.domain.subscription.api.dto.SubscriptionSummaryResponse;
import com.alrdream.domain.subscription.application.SubscriptionAdminService;
import com.alrdream.domain.subscription.domain.SubscriptionStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PagedModel;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Subscription (Admin)", description = "구독/사용량 대시보드 API — Admin 전용 [01] 13번, [03] §2-1")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/admin/subscriptions")
public class SubscriptionAdminController {

	private final SubscriptionAdminService subscriptionAdminService;

	public SubscriptionAdminController(SubscriptionAdminService subscriptionAdminService) {
		this.subscriptionAdminService = subscriptionAdminService;
	}

	@Operation(summary = "구독 목록 조회", description = "상태로 필터링할 수 있다. 생략하면 전체 상태를 반환한다.")
	@ApiResponse(responseCode = "200", description = "조회 성공")
	@GetMapping
	public ResponseEntity<PagedModel<SubscriptionAdminResponse>> list(
			@Parameter(description = "상태 필터") @RequestParam(required = false) SubscriptionStatus status,
			@ParameterObject
			@PageableDefault(size = 20, sort = "startedAt", direction = Sort.Direction.DESC) Pageable pageable) {
		Page<SubscriptionAdminResponse> page = subscriptionAdminService.list(status, pageable);
		return ResponseEntity.ok(new PagedModel<>(page));
	}

	@Operation(summary = "구독 현황 요약", description = "상태별(ACTIVE/PAST_DUE/CANCELED) 구독 수를 반환한다.")
	@ApiResponse(responseCode = "200", description = "조회 성공")
	@GetMapping("/summary")
	public ResponseEntity<SubscriptionSummaryResponse> summary() {
		return ResponseEntity.ok(subscriptionAdminService.summary());
	}
}
