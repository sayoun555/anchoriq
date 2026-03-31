package com.anchoriq.core.domain.account.subscription.model;

import java.util.Set;

/**
 * 구독 상태.
 * 상태 머신 패턴: 유효한 상태 전이만 허용한다.
 *
 * 상태 전이 규칙:
 * PENDING -> ACTIVE, CANCELLED
 * ACTIVE -> CANCELLED, EXPIRED
 * CANCELLED -> ACTIVE (재활성화)
 * EXPIRED -> ACTIVE (재구독)
 */
public enum SubscriptionStatus {

    PENDING(Set.of("ACTIVE", "CANCELLED")),
    ACTIVE(Set.of("CANCELLED", "EXPIRED")),
    CANCELLED(Set.of("ACTIVE")),
    EXPIRED(Set.of("ACTIVE"));

    private final Set<String> allowedTransitions;

    SubscriptionStatus(Set<String> allowedTransitions) {
        this.allowedTransitions = allowedTransitions;
    }

    public void validateTransitionTo(SubscriptionStatus target) {
        if (!allowedTransitions.contains(target.name())) {
            throw new IllegalStateException(
                    String.format("Invalid subscription status transition: %s -> %s", this.name(), target.name()));
        }
    }

    public boolean canTransitionTo(SubscriptionStatus target) {
        return allowedTransitions.contains(target.name());
    }
}
