package com.anchoriq.api.infrastructure.persistence.notification;

import com.anchoriq.core.domain.operation.notification.model.NotificationHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA 인터페이스 — notification_history 테이블.
 */
public interface JpaNotificationHistoryRepository extends JpaRepository<NotificationHistory, Long> {

    Page<NotificationHistory> findByRuleIdOrderBySentAtDesc(Long ruleId, Pageable pageable);

    Page<NotificationHistory> findAllByOrderBySentAtDesc(Pageable pageable);
}
