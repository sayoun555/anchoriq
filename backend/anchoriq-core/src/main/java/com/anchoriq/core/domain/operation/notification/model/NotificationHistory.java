package com.anchoriq.core.domain.operation.notification.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 알림 발송 이력 (불변 기록).
 */
@Entity
@Table(name = "notification_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rule_id")
    private Long ruleId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationChannel channel;

    @Column(nullable = false, length = 255)
    private String destination;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationStatus status;

    @Column(name = "sent_at", nullable = false, updatable = false)
    private LocalDateTime sentAt;

    private NotificationHistory(Long ruleId, NotificationChannel channel,
                                String destination, String message, NotificationStatus status) {
        this.ruleId = ruleId;
        this.channel = channel;
        this.destination = destination;
        this.message = message;
        this.status = status;
        this.sentAt = LocalDateTime.now();
    }

    public static NotificationHistory recordSent(Long ruleId, NotificationChannel channel,
                                                 String destination, String message) {
        return new NotificationHistory(ruleId, channel, destination, message, NotificationStatus.SENT);
    }

    public static NotificationHistory recordFailed(Long ruleId, NotificationChannel channel,
                                                   String destination, String message) {
        return new NotificationHistory(ruleId, channel, destination, message, NotificationStatus.FAILED);
    }

    public boolean isSent() {
        return this.status == NotificationStatus.SENT;
    }
}
