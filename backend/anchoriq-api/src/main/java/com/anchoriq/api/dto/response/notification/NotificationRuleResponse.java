package com.anchoriq.api.dto.response.notification;

import com.anchoriq.core.domain.operation.notification.model.NotificationRule;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 알림 규칙 응답 DTO.
 */
@Getter
@Builder
public class NotificationRuleResponse {

    private Long id;
    private String name;
    private String conditionType;
    private String conditionValue;
    private String channel;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static NotificationRuleResponse from(NotificationRule rule) {
        return NotificationRuleResponse.builder()
                .id(rule.getId())
                .name(rule.getName())
                .conditionType(rule.getConditionType())
                .conditionValue(rule.getConditionValue())
                .channel(rule.getChannel().name())
                .active(rule.isActive())
                .createdAt(rule.getCreatedAt())
                .updatedAt(rule.getUpdatedAt())
                .build();
    }
}
