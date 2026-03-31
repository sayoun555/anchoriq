package com.anchoriq.api.infrastructure.payment;

import com.anchoriq.core.domain.account.payment.gateway.PaymentGateway;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.net.Webhook;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.RefundCreateParams;
import lombok.extern.slf4j.Slf4j;

/**
 * Stripe 결제 게이트웨이 구현체.
 * Strategy 패턴 - PaymentGatewayRouter에서 통화 기반으로 선택된다.
 * Stripe Java SDK를 사용한 실제 API 호출 구조.
 * Bean 등록은 PaymentConfig에서 수행한다.
 */
@Slf4j
public class StripePaymentGateway implements PaymentGateway {

    private final String webhookSecret;

    public StripePaymentGateway(String secretKey, String webhookSecret) {
        Stripe.apiKey = secretKey;
        this.webhookSecret = webhookSecret;
    }

    @Override
    public PaymentGatewayResult charge(PaymentGatewayRequest request) {
        log.info("Stripe charge: amount={}, currency={}, email={}",
                request.amount(), request.currency(), request.customerEmail());
        try {
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(request.amount().longValue())
                    .setCurrency(request.currency().toLowerCase())
                    .setReceiptEmail(request.customerEmail())
                    .setDescription(request.description())
                    .setConfirm(true)
                    .setAutomaticPaymentMethods(
                            PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                    .setEnabled(true)
                                    .setAllowRedirects(
                                            PaymentIntentCreateParams.AutomaticPaymentMethods.AllowRedirects.NEVER)
                                    .build())
                    .build();

            PaymentIntent paymentIntent = PaymentIntent.create(params);
            log.info("Stripe payment successful: {}", paymentIntent.getId());
            return PaymentGatewayResult.success(paymentIntent.getId());
        } catch (StripeException e) {
            log.error("Stripe payment failed: code={}, message={}", e.getCode(), e.getMessage(), e);
            return PaymentGatewayResult.failure("Stripe payment failed: " + e.getMessage());
        }
    }

    @Override
    public void refund(String gatewayPaymentId) {
        log.info("Stripe refund: paymentId={}", gatewayPaymentId);
        try {
            RefundCreateParams params = RefundCreateParams.builder()
                    .setPaymentIntent(gatewayPaymentId)
                    .build();

            Refund refund = Refund.create(params);
            log.info("Stripe refund successful: refundId={}", refund.getId());
        } catch (StripeException e) {
            log.error("Stripe refund failed: code={}, message={}", e.getCode(), e.getMessage(), e);
            throw new RuntimeException("Stripe refund failed: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean verifyWebhookSignature(String payload, String signature) {
        log.debug("Verifying Stripe webhook signature");
        if (signature == null || signature.isBlank()) {
            return false;
        }
        try {
            Webhook.constructEvent(payload, signature, webhookSecret);
            return true;
        } catch (Exception e) {
            log.warn("Stripe webhook signature verification failed: {}", e.getMessage());
            return false;
        }
    }
}
