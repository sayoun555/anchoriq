package com.anchoriq.api.infrastructure.persistence.subscription;

import com.anchoriq.core.domain.account.subscription.model.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JpaSubscriptionRepository extends JpaRepository<Subscription, Long> {

    Optional<Subscription> findByUserId(Long userId);
}
