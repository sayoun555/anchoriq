package com.anchoriq.api.controller.payment;

import com.anchoriq.api.application.payment.PaymentApplicationService;
import com.anchoriq.api.dto.response.payment.PaymentResponse;
import com.anchoriq.api.global.response.ApiResponse;
import com.anchoriq.api.global.response.PageResponse;
import com.anchoriq.api.infrastructure.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentApplicationService paymentApplicationService;

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<PageResponse<PaymentResponse>>> getPaymentHistory(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<PaymentResponse> page = paymentApplicationService.getPaymentHistory(
                principal.userId(), pageable);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(page)));
    }
}
