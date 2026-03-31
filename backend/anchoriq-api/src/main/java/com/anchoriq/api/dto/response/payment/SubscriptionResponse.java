package com.anchoriq.api.dto.response.payment;

import com.anchoriq.core.domain.account.subscription.model.Subscription;

import java.time.LocalDateTime;

public record SubscriptionResponse(
        Long id,
        String plan,
        String status,
        LocalDateTime startedAt,
        LocalDateTime expiresAt
) {
    public static SubscriptionResponse from(Subscription subscription) {
        return new SubscriptionResponse(
                subscription.getId(),
                subscription.getPlan().name(),
                subscription.getStatus().name(),
                subscription.getStartedAt(),
                subscription.getExpiresAt()
        );
    }
}
