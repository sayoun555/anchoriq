package com.anchoriq.api.infrastructure.persistence.payment;

import com.anchoriq.core.domain.account.payment.model.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaPaymentRepository extends JpaRepository<Payment, Long> {

    Page<Payment> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}
