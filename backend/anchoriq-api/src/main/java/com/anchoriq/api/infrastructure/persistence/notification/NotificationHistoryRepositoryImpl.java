package com.anchoriq.api.infrastructure.persistence.notification;

import com.anchoriq.core.domain.operation.notification.model.NotificationHistory;
import com.anchoriq.core.domain.operation.notification.repository.NotificationHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

/**
 * NotificationHistoryRepository 구현체 — JPA 어댑터.
 */
@Repository
@RequiredArgsConstructor
public class NotificationHistoryRepositoryImpl implements NotificationHistoryRepository {

    private final JpaNotificationHistoryRepository jpaRepository;

    @Override
    public NotificationHistory save(NotificationHistory history) {
        return jpaRepository.save(history);
    }

    @Override
    public Page<NotificationHistory> findByRuleId(Long ruleId, Pageable pageable) {
        return jpaRepository.findByRuleIdOrderBySentAtDesc(ruleId, pageable);
    }

    @Override
    public Page<NotificationHistory> findAll(Pageable pageable) {
        return jpaRepository.findAllByOrderBySentAtDesc(pageable);
    }
}
