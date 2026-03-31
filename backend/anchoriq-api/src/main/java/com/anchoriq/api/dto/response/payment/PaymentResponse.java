package com.anchoriq.api.dto.response.payment;

import com.anchoriq.core.domain.account.payment.model.Payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentResponse(
        Long id,
        String gateway,
        BigDecimal amount,
        String currency,
        String status,
        LocalDateTime createdAt
) {
    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getGateway().name(),
                payment.getAmount(),
                payment.getCurrency().name(),
                payment.getStatus().name(),
                payment.getCreatedAt()
        );
    }
}
