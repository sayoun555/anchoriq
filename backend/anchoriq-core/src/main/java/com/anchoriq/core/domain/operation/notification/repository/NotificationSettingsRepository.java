package com.anchoriq.core.domain.operation.notification.repository;

import com.anchoriq.core.domain.operation.notification.model.NotificationSettings;

import java.util.Optional;

/**
 * 유저별 알림 설정 저장소 인터페이스.
 */
public interface NotificationSettingsRepository {

    NotificationSettings save(NotificationSettings settings);

    Optional<NotificationSettings> findByUserId(Long userId);
}
