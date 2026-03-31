package com.anchoriq.core.domain.account.subscription.factory;

import com.anchoriq.core.common.exception.DuplicateException;
import com.anchoriq.core.common.exception.EntityNotFoundException;
import com.anchoriq.core.domain.account.subscription.model.Plan;
import com.anchoriq.core.domain.account.subscription.model.Subscription;
import com.anchoriq.core.domain.account.subscription.repository.SubscriptionRepository;

/**
 * DDD Factory: 구독 생성 팩토리.
 * 기존 구독 중복 체크, 업그레이드 시 이전 구독 취소 등 복잡한 생성 로직을 캡슐화한다.
 * Bean 등록은 DomainFactoryConfig에서 수행한다.
 */
public class SubscriptionFactory {

    private final SubscriptionRepository subscriptionRepository;

    public SubscriptionFactory(SubscriptionRepository subscriptionRepository) {
        this.subscriptionRepository = subscriptionRepository;
    }

    /**
     * 무료 구독을 생성한다.
     * 기존 활성 구독이 있으면 DuplicateException을 던진다.
     */
    public Subscription createFreeSubscription(Long userId) {
        subscriptionRepository.findByUserId(userId)
                .ifPresent(s -> {
                    throw new DuplicateException("User already has a subscription");
                });

        return Subscription.createFree(userId);
    }

    /**
     * 구독을 업그레이드한다.
     * 기존 활성 구독을 취소하고 새 플랜으로 활성화한다.
     */
    public Subscription upgradeSubscription(Long userId, Plan newPlan) {
        Subscription current = subscriptionRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("Subscription", String.valueOf(userId)));

        current.activate(newPlan);
        return current;
    }
}
