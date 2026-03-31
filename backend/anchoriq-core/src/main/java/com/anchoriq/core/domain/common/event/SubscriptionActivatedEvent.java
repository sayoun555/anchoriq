package com.anchoriq.core.domain.common.event;

/**
 * 구독 활성화 도메인 이벤트.
 */
public class SubscriptionActivatedEvent extends DomainEvent {

    private final Long userId;
    private final String plan;

    public SubscriptionActivatedEvent(Long userId, String plan) {
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
        return String.format("SubscriptionActivated{userId=%d, plan='%s'}", userId, plan);
    }
}
