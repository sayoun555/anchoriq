package com.anchoriq.core.domain.common.event;

/**
 * 구독 취소 도메인 이벤트.
 */
public class SubscriptionCancelledEvent extends DomainEvent {

    private final Long userId;
    private final String plan;

    public SubscriptionCancelledEvent(Long userId, String plan) {
        super();
        this.userId = userId;
        this.plan = plan;
    }

    public Long getUserId() {
        return userId;
    }

    public String getPlan() {
        return plan;
    }

    @Override
    public String toString() {
        return String.format("SubscriptionCancelled{userId=%d, plan='%s'}", userId, plan);
    }
}
