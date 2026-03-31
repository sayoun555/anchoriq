package com.anchoriq.api.application.payment;

import com.anchoriq.api.dto.request.payment.SubscribeRequest;
import com.anchoriq.api.dto.response.payment.PaymentResponse;
import com.anchoriq.api.dto.response.payment.PlanResponse;
import com.anchoriq.api.dto.response.payment.SubscriptionResponse;
import com.anchoriq.api.infrastructure.payment.PaymentGatewayRouter;
import com.anchoriq.core.common.exception.EntityNotFoundException;
import com.anchoriq.core.common.exception.PaymentFailedException;
import com.anchoriq.core.domain.account.payment.gateway.PaymentGateway;
import com.anchoriq.core.domain.account.payment.gateway.PaymentGateway.PaymentGatewayRequest;
import com.anchoriq.core.domain.account.payment.gateway.PaymentGateway.PaymentGatewayResult;
import com.anchoriq.core.domain.account.payment.model.Currency;
import com.anchoriq.core.domain.account.payment.model.PaymentGatewayType;
import com.anchoriq.core.domain.account.payment.repository.PaymentRepository;
import com.anchoriq.core.domain.account.subscription.model.Plan;
import com.anchoriq.core.domain.account.subscription.model.Subscription;
import com.anchoriq.core.domain.account.subscription.repository.SubscriptionRepository;
import com.anchoriq.core.domain.account.user.model.User;
import com.anchoriq.core.domain.account.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentApplicationServiceImpl implements PaymentApplicationService {

    private final PaymentGatewayRouter paymentGatewayRouter;
    private final PaymentTransactionService paymentTransactionService;
    private final PaymentRepository paymentRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;

    @Override
    public SubscriptionResponse processSubscription(Long userId, SubscribeRequest request) {
        Plan plan = Plan.valueOf(request.plan().toUpperCase());
        Currency currency = Currency.valueOf(request.currency().toUpperCase());

        if (plan == Plan.FREE) {
            return activateFreeSubscription(userId);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User", String.valueOf(userId)));

        BigDecimal amount = calculatePrice(plan, currency);
        PaymentGateway gateway = paymentGatewayRouter.resolve(currency);
        PaymentGatewayType gatewayType = (currency == Currency.KRW)
                ? PaymentGatewayType.TOSS : PaymentGatewayType.STRIPE;

        PaymentGatewayResult gatewayResult = gateway.charge(
                new PaymentGatewayRequest(amount, currency.name(),
                        plan.name() + " subscription", user.getEmailValue()));

        if (!gatewayResult.success()) {
            throw new PaymentFailedException("Payment failed: " + gatewayResult.message());
        }

        try {
            return paymentTransactionService.savePaymentAndActivateSubscription(
                    userId, gatewayType, gatewayResult.gatewayPaymentId(), amount, currency, plan);
        } catch (Exception e) {
            log.error("DB save failed after payment success, initiating refund: paymentId={}",
                    gatewayResult.gatewayPaymentId(), e);
            try {
                gateway.refund(gatewayResult.gatewayPaymentId());
                log.info("Refund completed for paymentId={}", gatewayResult.gatewayPaymentId());
            } catch (Exception refundEx) {
                log.error("CRITICAL: Refund also failed for paymentId={}. Manual intervention required.",
                        gatewayResult.gatewayPaymentId(), refundEx);
            }
            throw new PaymentFailedException("Payment processing failed. Refund has been initiated.", e);
        }
    }

    @Override
    @Transactional
    public SubscriptionResponse cancelSubscription(Long userId) {
        Subscription subscription = subscriptionRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("Subscription", String.valueOf(userId)));

        subscription.cancel();
        Subscription saved = subscriptionRepository.save(subscription);

        log.info("Subscription cancelled: userId={}", userId);
        return SubscriptionResponse.from(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public SubscriptionResponse getSubscription(Long userId) {
        Subscription subscription = subscriptionRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("Subscription", String.valueOf(userId)));
        return SubscriptionResponse.from(subscription);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PaymentResponse> getPaymentHistory(Long userId, Pageable pageable) {
        return paymentRepository.findByUserId(userId, pageable)
                .map(PaymentResponse::from);
    }

    @Override
    public List<PlanResponse> getPlans() {
        return List.of(
                new PlanResponse("FREE", BigDecimal.ZERO, BigDecimal.ZERO,
                        List.of("Dashboard", "AI Query (5/day)"), 5),
                new PlanResponse("PRO", new BigDecimal("29.99"), new BigDecimal("39000"),
                        List.of("Dashboard", "Unlimited AI Query", "Real-time Alerts",
                                "Workflows", "Vessel History", "Export"), 100),
                new PlanResponse("ENTERPRISE", new BigDecimal("99.99"), new BigDecimal("130000"),
                        List.of("All PRO features", "What-if Simulation",
                                "External API", "Custom Ontology"), Integer.MAX_VALUE)
        );
    }

    @Transactional
    private SubscriptionResponse activateFreeSubscription(Long userId) {
        Subscription subscription = subscriptionRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("Subscription", String.valueOf(userId)));
        subscription.activate(Plan.FREE);
        Subscription saved = subscriptionRepository.save(subscription);
        return SubscriptionResponse.from(saved);
    }

    private BigDecimal calculatePrice(Plan plan, Currency currency) {
        return switch (plan) {
            case PRO -> (currency == Currency.KRW) ? new BigDecimal("39000") : new BigDecimal("29.99");
            case ENTERPRISE -> (currency == Currency.KRW) ? new BigDecimal("130000") : new BigDecimal("99.99");
            case FREE -> BigDecimal.ZERO;
        };
    }
}
