package com.anchoriq.core.domain.operation.notification.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 유저별 알림 설정 — 채널별 발송 대상(destination)을 보유.
 * 규칙(NotificationRule)은 "언제/어느 채널"을, 설정(NotificationSettings)은 "어디로 보낼지"를 담당한다.
 * 비즈니스 로직: destinationFor, hasDestinationFor
 */
@Entity
@Table(name = "notification_settings")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "slack_enabled", nullable = false)
    private boolean slackEnabled;

    @Column(name = "slack_webhook_url", length = 500)
    private String slackWebhookUrl;

    @Column(name = "email_enabled", nullable = false)
    private boolean emailEnabled;

    @Column(name = "email_address", length = 255)
    private String emailAddress;

    @Version
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private NotificationSettings(Long userId) {
        this.userId = userId;
        this.slackEnabled = false;
        this.emailEnabled = false;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 유저의 기본(빈) 설정 생성. 이후 update로 채운다.
     */
    public static NotificationSettings createDefault(Long userId) {
        return new NotificationSettings(userId);
    }

    /**
     * 설정 갱신 (업서트 시 사용).
     */
    public void update(boolean slackEnabled, String slackWebhookUrl,
                       boolean emailEnabled, String emailAddress) {
        this.slackEnabled = slackEnabled;
        this.slackWebhookUrl = slackWebhookUrl;
        this.emailEnabled = emailEnabled;
        this.emailAddress = emailAddress;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 채널의 발송 대상(destination)을 반환. 해당 채널이 비활성이거나 미설정이면 null.
     * Tell-Don't-Ask: 목적지 결정을 도메인이 책임진다.
     */
    public String destinationFor(NotificationChannel channel) {
        return switch (channel) {
            case SLACK -> slackEnabled ? slackWebhookUrl : null;
            case EMAIL -> emailEnabled ? emailAddress : null;
        };
    }

    /**
     * 해당 채널로 실제 발송 가능한 목적지가 설정돼 있는지.
     */
    public boolean hasDestinationFor(NotificationChannel channel) {
        String destination = destinationFor(channel);
        return destination != null && !destination.isBlank();
    }
}
