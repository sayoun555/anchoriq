package com.anchoriq.api.infrastructure.payment;

import com.anchoriq.core.domain.account.payment.gateway.PaymentGateway;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

/**
 * Toss Payments 결제 게이트웨이 구현체.
 * Strategy 패턴 - PaymentGatewayRouter에서 통화 기반으로 선택된다.
 * RestClient를 통한 Toss Payments REST API 호출 구조.
 * Bean 등록은 PaymentConfig에서 수행한다.
 */
@Slf4j
public class TossPaymentGateway implements PaymentGateway {

    private static final String TOSS_API_BASE = "https://api.tosspayments.com/v1";

    private final String webhookSecret;
    private final RestClient restClient;

    public TossPaymentGateway(String secretKey, String webhookSecret) {
        this.webhookSecret = webhookSecret;
        String encodedKey = Base64.getEncoder()
                .encodeToString((secretKey + ":").getBytes(StandardCharsets.UTF_8));
        this.restClient = RestClient.builder()
                .baseUrl(TOSS_API_BASE)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Basic " + encodedKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public PaymentGatewayResult charge(PaymentGatewayRequest request) {
        log.info("Toss charge: amount={}, currency={}, email={}",
                request.amount(), request.currency(), request.customerEmail());
        try {
            String orderId = "order_" + System.currentTimeMillis();

            Map<String, Object> body = Map.of(
                    "orderId", orderId,
                    "amount", request.amount().intValue(),
                    "orderName", request.description()
            );

            Map<?, ?> response = restClient.post()
                    .uri("/payments/confirm")
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            String paymentKey = response != null ? String.valueOf(response.get("paymentKey")) : orderId;
            log.info("Toss payment successful: {}", paymentKey);
            return PaymentGatewayResult.success(paymentKey);
        } catch (Exception e) {
            log.error("Toss payment failed: {}", e.getMessage(), e);
            return PaymentGatewayResult.failure("Toss payment failed: " + e.getMessage());
        }
    }

    @Override
    public void refund(String gatewayPaymentId) {
        log.info("Toss refund: paymentKey={}", gatewayPaymentId);
        try {
            Map<String, Object> body = Map.of(
                    "cancelReason", "Customer request"
            );

            restClient.post()
                    .uri("/payments/{paymentKey}/cancel", gatewayPaymentId)
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            log.info("Toss refund successful for: {}", gatewayPaymentId);
        } catch (Exception e) {
            log.error("Toss refund failed: {}", e.getMessage(), e);
            throw new RuntimeException("Toss refund failed: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean verifyWebhookSignature(String payload, String signature) {
        log.debug("Verifying Toss webhook signature");
        if (signature == null || signature.isBlank()) {
            return false;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String computed = Base64.getEncoder().encodeToString(hash);
            return computed.equals(signature);
        } catch (Exception e) {
            log.warn("Toss webhook signature verification failed: {}", e.getMessage());
            return false;
        }
    }
}
