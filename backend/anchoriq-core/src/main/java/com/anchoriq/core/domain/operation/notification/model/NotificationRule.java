package com.anchoriq.core.domain.operation.notification.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 알림 규칙 — 조건 매칭 시 지정 채널로 알림 발송.
 * 비즈니스 로직: matches, isActive, isOwnedBy
 */
@Entity
@Table(name = "notification_rules")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "condition_type", nullable = false, length = 50)
    private String conditionType;

    @Column(name = "condition_value", nullable = false, length = 255)
    private String conditionValue;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationChannel channel;

    @Column(nullable = false)
    private boolean active;

    @Version
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private NotificationRule(Long userId, String name, String conditionType,
                             String conditionValue, NotificationChannel channel) {
        this.userId = userId;
        this.name = name;
        this.conditionType = conditionType;
        this.conditionValue = conditionValue;
        this.channel = channel;
        this.active = true;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public static NotificationRule create(Long userId, String name, String conditionType,
                                          String conditionValue, NotificationChannel channel) {
        return new NotificationRule(userId, name, conditionType, conditionValue, channel);
    }

    /**
     * 이벤트가 이 규칙의 조건에 매칭되는지 판단.
     */
    public boolean matches(String eventType, String riskLevel) {
        if (!this.active) {
            return false;
        }
        return matchesCondition(eventType, riskLevel);
    }

    private boolean matchesCondition(String eventType, String riskLevel) {
        return switch (this.conditionType) {
            case "RISK_LEVEL" -> this.conditionValue.equalsIgnoreCase(riskLevel);
            case "EVENT_TYPE" -> this.conditionValue.equalsIgnoreCase(eventType);
            case "ALL" -> true;
            default -> false;
        };
    }

    /**
     * 규칙이 활성 상태인지 확인.
     */
    public boolean isActive() {
        return this.active;
    }

    /**
     * 소유자 확인.
     */
    public boolean isOwnedBy(Long userId) {
        return this.userId.equals(userId);
    }

    /**
     * 규칙 활성화.
     */
    public void enable() {
        this.active = true;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 규칙 비활성화.
     */
    public void disable() {
        this.active = false;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 규칙 정보 수정.
     */
    public void update(String name, String conditionType, String conditionValue,
                       NotificationChannel channel) {
        this.name = name;
        this.conditionType = conditionType;
        this.conditionValue = conditionValue;
        this.channel = channel;
        this.updatedAt = LocalDateTime.now();
    }
}
