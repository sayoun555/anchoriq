package com.anchoriq.core.domain.account.subscription.model;

import com.anchoriq.core.domain.common.event.DomainEvent;
import com.anchoriq.core.domain.common.event.SubscriptionActivatedEvent;
import com.anchoriq.core.domain.common.event.SubscriptionCancelledEvent;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 구독 엔티티.
 * 상태 머신 패턴을 통해 유효한 상태 전이만 허용한다.
 * 도메인 이벤트를 수집하여 Application Service에서 발행한다.
 */
@Entity
@Table(name = "subscriptions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Subscription {

    @Transient
    private final List<DomainEvent> domainEvents = new ArrayList<>();

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Plan plan;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SubscriptionStatus status;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "daily_api_usage")
    private int dailyApiUsage;

    @Version
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private Subscription(Long userId, Plan plan) {
        this.userId = userId;
        this.plan = plan;
        this.status = SubscriptionStatus.PENDING;
        this.startedAt = LocalDateTime.now();
        this.dailyApiUsage = 0;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public static Subscription createFree(Long userId) {
        Subscription sub = new Subscription(userId, Plan.FREE);
        sub.status = SubscriptionStatus.ACTIVE;
        return sub;
    }

    public static Subscription createPending(Long userId, Plan plan) {
        return new Subscription(userId, plan);
    }

    // --- 상태 머신 ---

    public boolean isActive() {
        if (status != SubscriptionStatus.ACTIVE) {
            return false;
        }
        return expiresAt == null || !expiresAt.isBefore(LocalDateTime.now());
    }

    public boolean canAccess(Feature feature) {
        return isActive() && plan.supports(feature);
    }

    /**
     * 구독을 활성화한다.
     * PENDING 또는 CANCELLED/EXPIRED 상태에서만 ACTIVE로 전이 가능하다.
     */
    public void activate(Plan newPlan) {
        this.status.validateTransitionTo(SubscriptionStatus.ACTIVE);
        this.plan = newPlan;
        this.status = SubscriptionStatus.ACTIVE;
        this.startedAt = LocalDateTime.now();
        this.expiresAt = (newPlan == Plan.FREE) ? null : LocalDateTime.now().plusDays(30);
        this.dailyApiUsage = 0;
        this.updatedAt = LocalDateTime.now();
        domainEvents.add(new SubscriptionActivatedEvent(this.userId, newPlan.name()));
    }

    /**
     * 구독을 취소한다.
     * ACTIVE 상태에서만 CANCELLED로 전이 가능하다.
     */
    public void cancel() {
        this.status.validateTransitionTo(SubscriptionStatus.CANCELLED);
        this.status = SubscriptionStatus.CANCELLED;
        this.updatedAt = LocalDateTime.now();
        domainEvents.add(new SubscriptionCancelledEvent(this.userId, this.plan.name()));
    }

    /**
     * 구독을 갱신한다.
     */
    public void renew() {
        if (!isActive()) {
            throw new IllegalStateException("Cannot renew inactive subscription");
        }
        this.expiresAt = LocalDateTime.now().plusDays(30);
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 취소/만료된 구독을 재활성화한다.
     */
    public void reactivate(Plan plan) {
        this.status.validateTransitionTo(SubscriptionStatus.ACTIVE);
        this.plan = plan;
        this.status = SubscriptionStatus.ACTIVE;
        this.startedAt = LocalDateTime.now();
        this.expiresAt = (plan == Plan.FREE) ? null : LocalDateTime.now().plusDays(30);
        this.dailyApiUsage = 0;
        this.updatedAt = LocalDateTime.now();
        domainEvents.add(new SubscriptionActivatedEvent(this.userId, plan.name()));
    }

    // --- 비즈니스 로직 ---

    /**
     * API 사용량을 체크하고 차감한다 (원자적).
     * 일일 한도를 초과하면 IllegalStateException을 던진다.
     */
    public void checkAndDecrementQuota() {
        if (!isActive()) {
            throw new IllegalStateException("Subscription is not active");
        }
        if (dailyApiUsage >= plan.getDailyApiLimit()) {
            throw new IllegalStateException(
                    String.format("Daily API limit exceeded: %d/%d", dailyApiUsage, plan.getDailyApiLimit()));
        }
        this.dailyApiUsage++;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 결제 성공 시 플랜을 활성화한다.
     */
    public void handlePaymentSuccess(Plan paidPlan) {
        if (this.status == SubscriptionStatus.PENDING
                || this.status == SubscriptionStatus.CANCELLED
                || this.status == SubscriptionStatus.EXPIRED) {
            activate(paidPlan);
        } else if (this.status == SubscriptionStatus.ACTIVE) {
            // 업그레이드
            this.plan = paidPlan;
            this.expiresAt = LocalDateTime.now().plusDays(30);
            this.updatedAt = LocalDateTime.now();
        }
    }

    /**
     * 결제 실패 시 FREE 플랜으로 다운그레이드한다.
     */
    public void handlePaymentFailure() {
        this.plan = Plan.FREE;
        this.expiresAt = null;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 일일 사용량을 리셋한다 (스케줄러 호출용).
     */
    public void resetDailyUsage() {
        this.dailyApiUsage = 0;
        this.updatedAt = LocalDateTime.now();
    }

    // --- 이벤트 ---

    public List<DomainEvent> getDomainEvents() {
        return Collections.unmodifiableList(domainEvents);
    }

    public void clearDomainEvents() {
        domainEvents.clear();
    }
}
