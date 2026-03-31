package com.anchoriq.api.application.payment;

import com.anchoriq.api.dto.response.payment.SubscriptionResponse;
import com.anchoriq.core.common.exception.EntityNotFoundException;
import com.anchoriq.core.domain.account.payment.model.Currency;
import com.anchoriq.core.domain.account.payment.model.Payment;
import com.anchoriq.core.domain.account.payment.model.PaymentGatewayType;
import com.anchoriq.core.domain.account.payment.repository.PaymentRepository;
import com.anchoriq.core.domain.account.subscription.model.Plan;
import com.anchoriq.core.domain.account.subscription.model.Subscription;
import com.anchoriq.core.domain.account.subscription.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentTransactionService {

    private final PaymentRepository paymentRepository;
    private final SubscriptionRepository subscriptionRepository;

    @Transactional
    public SubscriptionResponse savePaymentAndActivateSubscription(
            Long userId, PaymentGatewayType gatewayType, String gatewayPaymentId,
            BigDecimal amount, Currency currency, Plan plan) {

        Payment payment = Payment.createSuccess(userId, gatewayType, gatewayPaymentId, amount, currency);
        paymentRepository.save(payment);

        Subscription subscription = subscriptionRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("Subscription", String.valueOf(userId)));
        subscription.activate(plan);
        Subscription savedSubscription = subscriptionRepository.save(subscription);

        log.info("Subscription activated: userId={}, plan={}", userId, plan);
        return SubscriptionResponse.from(savedSubscription);
    }
}
