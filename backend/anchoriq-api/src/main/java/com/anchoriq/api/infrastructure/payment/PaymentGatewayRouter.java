package com.anchoriq.api.infrastructure.payment;

import com.anchoriq.core.domain.account.payment.gateway.PaymentGateway;
import com.anchoriq.core.domain.account.payment.model.Currency;

/**
 * 통화 기반으로 결제 게이트웨이를 선택하는 라우터.
 * Bean 등록은 PaymentConfig에서 수행한다.
 */
public class PaymentGatewayRouter {

    private final StripePaymentGateway stripePaymentGateway;
    private final TossPaymentGateway tossPaymentGateway;

    public PaymentGatewayRouter(StripePaymentGateway stripePaymentGateway,
                                 TossPaymentGateway tossPaymentGateway) {
        this.stripePaymentGateway = stripePaymentGateway;
        this.tossPaymentGateway = tossPaymentGateway;
    }

    public PaymentGateway resolve(Currency currency) {
        return switch (currency) {
            case KRW -> tossPaymentGateway;
            case USD -> stripePaymentGateway;
        };
    }
}
