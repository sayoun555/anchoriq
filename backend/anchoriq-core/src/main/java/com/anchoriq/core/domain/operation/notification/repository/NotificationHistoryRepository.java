package com.anchoriq.core.domain.operation.notification.repository;

import com.anchoriq.core.domain.operation.notification.model.NotificationHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * 알림 발송 이력 저장소 인터페이스.
 */
public interface NotificationHistoryRepository {

    NotificationHistory save(NotificationHistory history);

    Page<NotificationHistory> findByRuleId(Long ruleId, Pageable pageable);

    Page<NotificationHistory> findAll(Pageable pageable);
}
