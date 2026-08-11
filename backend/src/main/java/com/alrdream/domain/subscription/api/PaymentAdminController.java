package com.alrdream.domain.subscription.api;

import com.alrdream.domain.subscription.api.dto.PaymentAdminResponse;
import com.alrdream.domain.subscription.application.PaymentAdminService;
import com.alrdream.domain.subscription.domain.PaymentStatus;
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

@Tag(name = "Payment (Admin)", description = "결제 관리 API — Admin 전용 [03] §2-1")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/admin/payments")
public class PaymentAdminController {

	private final PaymentAdminService paymentAdminService;

	public PaymentAdminController(PaymentAdminService paymentAdminService) {
		this.paymentAdminService = paymentAdminService;
	}

	@Operation(summary = "결제 내역 목록 조회", description = "상태로 필터링할 수 있다. 생략하면 전체 상태를 반환한다.")
	@ApiResponse(responseCode = "200", description = "조회 성공")
	@GetMapping
	public ResponseEntity<PagedModel<PaymentAdminResponse>> list(
			@Parameter(description = "상태 필터") @RequestParam(required = false) PaymentStatus status,
			@ParameterObject
			@PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
		Page<PaymentAdminResponse> page = paymentAdminService.list(status, pageable);
		return ResponseEntity.ok(new PagedModel<>(page));
	}
}
