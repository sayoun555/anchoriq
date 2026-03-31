package com.anchoriq.core.domain.common.event;

import java.math.BigDecimal;

/**
 * 결제 완료 도메인 이벤트.
 */
public class PaymentCompletedEvent extends DomainEvent {

    private final Long userId;
    private final BigDecimal amount;
    private final String currency;
    private final String gatewayPaymentId;

    public PaymentCompletedEvent(Long userId, BigDecimal amount, String currency, String gatewayPaymentId) {
        super();
        this.userId = userId;
        this.amount = amount;
        this.currency = currency;
        this.gatewayPaymentId = gatewayPaymentId;
    }

    public Long getUserId() {
        return userId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public String getGatewayPaymentId() {
        return gatewayPaymentId;
    }

    @Override
    public String toString() {
        return String.format("PaymentCompleted{userId=%d, amount=%s %s}", userId, amount, currency);
    }
}
