package com.alrdream.domain.subscription.domain;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentHistoryRepository extends JpaRepository<PaymentHistory, UUID> {

	boolean existsByPaymentId(String paymentId);
}
