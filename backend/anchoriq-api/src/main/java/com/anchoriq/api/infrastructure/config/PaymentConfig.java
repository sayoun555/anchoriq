package com.anchoriq.api.infrastructure.config;

import com.anchoriq.api.infrastructure.payment.PaymentGatewayRouter;
import com.anchoriq.api.infrastructure.payment.StripePaymentGateway;
import com.anchoriq.api.infrastructure.payment.TossPaymentGateway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 결제 게이트웨이 Bean 설정.
 * Strategy 패턴으로 Stripe/Toss를 교체 가능하게 관리한다.
 */
@Configuration
public class PaymentConfig {

    @Bean
    public StripePaymentGateway stripePaymentGateway(
            @Value("${stripe.secret-key}") String secretKey,
            @Value("${stripe.webhook-secret}") String webhookSecret) {
        return new StripePaymentGateway(secretKey, webhookSecret);
    }

    @Bean
    public TossPaymentGateway tossPaymentGateway(
            @Value("${toss.secret-key}") String secretKey,
            @Value("${toss.webhook-secret}") String webhookSecret) {
        return new TossPaymentGateway(secretKey, webhookSecret);
    }

    @Bean
    public PaymentGatewayRouter paymentGatewayRouter(
            StripePaymentGateway stripePaymentGateway,
            TossPaymentGateway tossPaymentGateway) {
        return new PaymentGatewayRouter(stripePaymentGateway, tossPaymentGateway);
    }
}
