package com.anchoriq.api.infrastructure.persistence.notification;

import com.anchoriq.core.domain.operation.notification.model.NotificationRule;
import com.anchoriq.core.domain.operation.notification.repository.NotificationRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * NotificationRuleRepository 구현체 — JPA 어댑터.
 */
@Repository
@RequiredArgsConstructor
public class NotificationRuleRepositoryImpl implements NotificationRuleRepository {

    private final JpaNotificationRuleRepository jpaRepository;

    @Override
    public NotificationRule save(NotificationRule rule) {
        return jpaRepository.save(rule);
    }

    @Override
    public Optional<NotificationRule> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public Page<NotificationRule> findByUserId(Long userId, Pageable pageable) {
        return jpaRepository.findByUserId(userId, pageable);
    }

    @Override
    public List<NotificationRule> findActiveRules() {
        return jpaRepository.findByActiveTrue();
    }

    @Override
    public List<NotificationRule> findActiveRulesByUserId(Long userId) {
        return jpaRepository.findByUserIdAndActiveTrue(userId);
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }
}
