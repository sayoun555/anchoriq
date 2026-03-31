package com.anchoriq.core.domain.account.subscription.repository;

import com.anchoriq.core.domain.account.subscription.model.Subscription;

import java.util.Optional;

public interface SubscriptionRepository {

    Subscription save(Subscription subscription);

    Optional<Subscription> findById(Long id);

    Optional<Subscription> findByUserId(Long userId);
}
