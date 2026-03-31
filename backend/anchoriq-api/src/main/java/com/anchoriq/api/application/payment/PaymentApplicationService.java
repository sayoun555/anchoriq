package com.anchoriq.api.application.payment;

import com.anchoriq.api.dto.request.payment.SubscribeRequest;
import com.anchoriq.api.dto.response.payment.PaymentResponse;
import com.anchoriq.api.dto.response.payment.PlanResponse;
import com.anchoriq.api.dto.response.payment.SubscriptionResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * 결제 Application Service 인터페이스.
 */
public interface PaymentApplicationService {

    SubscriptionResponse processSubscription(Long userId, SubscribeRequest request);

    SubscriptionResponse cancelSubscription(Long userId);

    SubscriptionResponse getSubscription(Long userId);

    Page<PaymentResponse> getPaymentHistory(Long userId, Pageable pageable);

    List<PlanResponse> getPlans();
}
