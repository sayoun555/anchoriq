package com.anchoriq.core.domain.account.payment.gateway;

import java.math.BigDecimal;

public interface PaymentGateway {

    PaymentGatewayResult charge(PaymentGatewayRequest request);

    void refund(String gatewayPaymentId);

    boolean verifyWebhookSignature(String payload, String signature);

    record PaymentGatewayRequest(
            BigDecimal amount,
            String currency,
            String description,
            String customerEmail
    ) {}

    record PaymentGatewayResult(
            String gatewayPaymentId,
            boolean success,
            String message
    ) {
        public static PaymentGatewayResult success(String gatewayPaymentId) {
            return new PaymentGatewayResult(gatewayPaymentId, true, "Payment successful");
        }

        public static PaymentGatewayResult failure(String message) {
            return new PaymentGatewayResult(null, false, message);
        }
    }
}
