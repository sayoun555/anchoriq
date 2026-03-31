package com.anchoriq.api.infrastructure.persistence.notification;

import com.anchoriq.core.domain.operation.notification.model.NotificationRule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Spring Data JPA 인터페이스 — notification_rules 테이블.
 */
public interface JpaNotificationRuleRepository extends JpaRepository<NotificationRule, Long> {

    Page<NotificationRule> findByUserId(Long userId, Pageable pageable);

    List<NotificationRule> findByActiveTrue();

    List<NotificationRule> findByUserIdAndActiveTrue(Long userId);
}
