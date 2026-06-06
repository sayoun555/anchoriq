package com.anchoriq.api.infrastructure.persistence.notification;

import com.anchoriq.core.domain.operation.notification.model.NotificationSettings;
import com.anchoriq.core.domain.operation.notification.repository.NotificationSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * NotificationSettingsRepository 구현체 — JPA 어댑터.
 */
@Repository
@RequiredArgsConstructor
public class NotificationSettingsRepositoryImpl implements NotificationSettingsRepository {

    private final JpaNotificationSettingsRepository jpaRepository;

    @Override
    public NotificationSettings save(NotificationSettings settings) {
        return jpaRepository.save(settings);
    }

    @Override
    public Optional<NotificationSettings> findByUserId(Long userId) {
        return jpaRepository.findByUserId(userId);
    }
}
