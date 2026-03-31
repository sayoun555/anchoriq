package com.anchoriq.api.infrastructure.persistence.subscription;

import com.anchoriq.core.domain.account.subscription.model.Subscription;
import com.anchoriq.core.domain.account.subscription.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class SubscriptionRepositoryImpl implements SubscriptionRepository {

    private final JpaSubscriptionRepository jpaSubscriptionRepository;

    @Override
    public Subscription save(Subscription subscription) {
        return jpaSubscriptionRepository.save(subscription);
    }

    @Override
    public Optional<Subscription> findById(Long id) {
        return jpaSubscriptionRepository.findById(id);
    }

    @Override
    public Optional<Subscription> findByUserId(Long userId) {
        return jpaSubscriptionRepository.findByUserId(userId);
    }
}
