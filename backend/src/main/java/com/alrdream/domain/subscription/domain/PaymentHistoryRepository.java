package com.alrdream.domain.subscription.domain;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentHistoryRepository extends JpaRepository<PaymentHistory, UUID> {

	boolean existsByPaymentId(String paymentId);

	/** Phase 16 — Admin 대시보드 통계("이번 달 결제 성공/실패 건수"). */
	long countByStatusAndCreatedAtAfter(PaymentStatus status, OffsetDateTime after);
}
