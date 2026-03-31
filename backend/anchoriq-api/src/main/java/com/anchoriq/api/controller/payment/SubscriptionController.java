package com.anchoriq.api.controller.payment;

import com.anchoriq.api.application.payment.PaymentApplicationService;
import com.anchoriq.api.dto.request.payment.SubscribeRequest;
import com.anchoriq.api.dto.response.payment.PlanResponse;
import com.anchoriq.api.dto.response.payment.SubscriptionResponse;
import com.anchoriq.api.global.response.ApiResponse;
import com.anchoriq.api.infrastructure.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class SubscriptionController {

    private final PaymentApplicationService paymentApplicationService;

    @GetMapping("/plans")
    public ResponseEntity<ApiResponse<List<PlanResponse>>> getPlans() {
        List<PlanResponse> plans = paymentApplicationService.getPlans();
        return ResponseEntity.ok(ApiResponse.success(plans));
    }

    @PostMapping("/subscribe")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> subscribe(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody SubscribeRequest request) {
        SubscriptionResponse response = paymentApplicationService.processSubscription(
                principal.userId(), request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/cancel")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> cancel(
            @AuthenticationPrincipal UserPrincipal principal) {
        SubscriptionResponse response = paymentApplicationService.cancelSubscription(principal.userId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/subscription")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> getSubscription(
            @AuthenticationPrincipal UserPrincipal principal) {
        SubscriptionResponse response = paymentApplicationService.getSubscription(principal.userId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
