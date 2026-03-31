package com.anchoriq.core.domain.operation.notification.repository;

import com.anchoriq.core.domain.operation.notification.model.NotificationRule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

/**
 * 알림 규칙 저장소 인터페이스.
 */
public interface NotificationRuleRepository {

    NotificationRule save(NotificationRule rule);

    Optional<NotificationRule> findById(Long id);

    Page<NotificationRule> findByUserId(Long userId, Pageable pageable);

    List<NotificationRule> findActiveRules();

    List<NotificationRule> findActiveRulesByUserId(Long userId);

    void deleteById(Long id);
}
