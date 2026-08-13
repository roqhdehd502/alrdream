package com.alrdream.domain.subscription.api;

import com.alrdream.domain.subscription.api.dto.PaymentAdminResponse;
import com.alrdream.domain.subscription.application.PaymentAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PagedModel;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Payment (Admin)", description = "사용자별 결제 내역 CS 조회 API — Admin 전용 [03] §2-1")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/admin/users/{userId}/payments")
public class UserPaymentAdminController {

	private final PaymentAdminService paymentAdminService;

	public UserPaymentAdminController(PaymentAdminService paymentAdminService) {
		this.paymentAdminService = paymentAdminService;
	}

	@Operation(summary = "사용자별 결제 내역 조회", description = "해지 후 재구독으로 여러 구독 이력이 있어도 전체 통합 조회한다. CS 대응용 — 수정/삭제는 지원하지 않는다.")
	@ApiResponse(responseCode = "200", description = "조회 성공")
	@GetMapping
	public ResponseEntity<PagedModel<PaymentAdminResponse>> list(
			@PathVariable UUID userId,
			@ParameterObject
			@PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
		Page<PaymentAdminResponse> page = paymentAdminService.listForUser(userId, pageable);
		return ResponseEntity.ok(new PagedModel<>(page));
	}
}
