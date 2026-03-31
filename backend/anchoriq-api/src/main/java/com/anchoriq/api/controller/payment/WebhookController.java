package com.anchoriq.api.controller.payment;

import com.anchoriq.api.global.response.ApiResponse;
import com.anchoriq.api.infrastructure.payment.StripePaymentGateway;
import com.anchoriq.api.infrastructure.payment.TossPaymentGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/payments/webhook")
@RequiredArgsConstructor
public class WebhookController {

    private final StripePaymentGateway stripePaymentGateway;
    private final TossPaymentGateway tossPaymentGateway;

    @PostMapping("/stripe")
    public ResponseEntity<ApiResponse<Void>> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "Stripe-Signature", required = false) String signature) {

        if (!stripePaymentGateway.verifyWebhookSignature(payload, signature)) {
            log.warn("Invalid Stripe webhook signature");
            return ResponseEntity.badRequest().body(ApiResponse.error("INVALID_SIGNATURE", "Invalid signature"));
        }

        log.info("Stripe webhook received");
        // Process webhook event (subscription renewal, payment failure, etc.)
        return ResponseEntity.ok(ApiResponse.success());
    }

    @PostMapping("/toss")
    public ResponseEntity<ApiResponse<Void>> handleTossWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "Toss-Signature", required = false) String signature) {

        if (!tossPaymentGateway.verifyWebhookSignature(payload, signature)) {
            log.warn("Invalid Toss webhook signature");
            return ResponseEntity.badRequest().body(ApiResponse.error("INVALID_SIGNATURE", "Invalid signature"));
        }

        log.info("Toss webhook received");
        // Process webhook event
        return ResponseEntity.ok(ApiResponse.success());
    }
}
