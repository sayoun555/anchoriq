package com.anchoriq.core.domain.account.subscription.service;

import com.anchoriq.core.domain.account.subscription.model.Feature;
import com.anchoriq.core.domain.account.subscription.model.Subscription;

public interface SubscriptionService {

    boolean canUseFeature(Long userId, Feature feature);

    boolean hasRemainingApiQuota(Long userId);

    void incrementApiUsage(Long userId);

    Subscription getActiveSubscription(Long userId);
}
