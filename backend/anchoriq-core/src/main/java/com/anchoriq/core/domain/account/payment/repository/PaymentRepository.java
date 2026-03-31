package com.anchoriq.core.domain.account.payment.repository;

import com.anchoriq.core.domain.account.payment.model.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface PaymentRepository {

    Payment save(Payment payment);

    Optional<Payment> findById(Long id);

    Page<Payment> findByUserId(Long userId, Pageable pageable);
}
