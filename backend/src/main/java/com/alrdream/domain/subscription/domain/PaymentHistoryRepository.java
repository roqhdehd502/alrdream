package com.alrdream.domain.subscription.domain;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentHistoryRepository extends JpaRepository<PaymentHistory, UUID> {

	boolean existsByPaymentId(String paymentId);

	/** Phase 16 — Admin 대시보드 통계("이번 달 결제 성공/실패 건수"). */
	long countByStatusAndCreatedAtAfter(PaymentStatus status, OffsetDateTime after);

	/** Phase 18 후속 — 사용자 본인의 결제 내역 조회. */
	List<PaymentHistory> findAllBySubscriptionIdInOrderByCreatedAtDesc(List<UUID> subscriptionIds);

	/** Phase 18 후속 — Admin "결제 관리" 전체 목록(상태 필터 있음). */
	Page<PaymentHistory> findAllByStatus(PaymentStatus status, Pageable pageable);

	/** Phase 18 후속 — Admin 사용자 상세의 결제 내역(해지 후 재구독 이력 포함). */
	Page<PaymentHistory> findAllBySubscriptionIdIn(List<UUID> subscriptionIds, Pageable pageable);
}
