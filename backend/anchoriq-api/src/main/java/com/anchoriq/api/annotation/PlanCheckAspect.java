package com.anchoriq.api.annotation;

import com.anchoriq.api.infrastructure.security.UserPrincipal;
import com.anchoriq.core.common.exception.PlanLimitExceededException;
import com.anchoriq.core.domain.account.subscription.model.Plan;
import com.anchoriq.core.domain.account.subscription.model.Subscription;
import com.anchoriq.core.domain.account.subscription.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
public class PlanCheckAspect {

    private final SubscriptionService subscriptionService;

    @Before("@annotation(requiresPlan)")
    public void checkPlan(RequiresPlan requiresPlan) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new PlanLimitExceededException("Authentication required");
        }

        Subscription subscription = subscriptionService.getActiveSubscription(principal.userId());
        Plan requiredPlan = requiresPlan.value();

        if (!meetsMinimumPlan(subscription.getPlan(), requiredPlan)) {
            throw new PlanLimitExceededException(
                    String.format("This feature requires %s plan or higher. Please upgrade.", requiredPlan.name()));
        }
    }

    private boolean meetsMinimumPlan(Plan currentPlan, Plan requiredPlan) {
        return currentPlan.ordinal() >= requiredPlan.ordinal();
    }
}
