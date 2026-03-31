package com.anchoriq.core.domain.account.payment.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentGatewayType gateway;

    @Column(name = "gateway_payment_id")
    private String gatewayPaymentId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    private Currency currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private Payment(Long userId, PaymentGatewayType gateway, String gatewayPaymentId,
                    BigDecimal amount, Currency currency, PaymentStatus status) {
        this.userId = userId;
        this.gateway = gateway;
        this.gatewayPaymentId = gatewayPaymentId;
        this.amount = amount;
        this.currency = currency;
        this.status = status;
        this.createdAt = LocalDateTime.now();
    }

    public static Payment createSuccess(Long userId, PaymentGatewayType gateway,
                                        String gatewayPaymentId, BigDecimal amount, Currency currency) {
        return new Payment(userId, gateway, gatewayPaymentId, amount, currency, PaymentStatus.SUCCESS);
    }

    public static Payment createFailed(Long userId, PaymentGatewayType gateway,
                                       BigDecimal amount, Currency currency) {
        return new Payment(userId, gateway, null, amount, currency, PaymentStatus.FAILED);
    }

    public void markRefunded() {
        if (this.status != PaymentStatus.SUCCESS) {
            throw new IllegalStateException("Only successful payments can be refunded");
        }
        this.status = PaymentStatus.REFUNDED;
    }

    public boolean isSuccess() {
        return this.status == PaymentStatus.SUCCESS;
    }
}
