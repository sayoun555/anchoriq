package com.anchoriq.api.infrastructure.persistence.payment;

import com.anchoriq.core.domain.account.payment.model.Payment;
import com.anchoriq.core.domain.account.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PaymentRepositoryImpl implements PaymentRepository {

    private final JpaPaymentRepository jpaPaymentRepository;

    @Override
    public Payment save(Payment payment) {
        return jpaPaymentRepository.save(payment);
    }

    @Override
    public Optional<Payment> findById(Long id) {
        return jpaPaymentRepository.findById(id);
    }

    @Override
    public Page<Payment> findByUserId(Long userId, Pageable pageable) {
        return jpaPaymentRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }
}
