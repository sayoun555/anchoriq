package com.anchoriq.api.dto.response.notification;

import com.anchoriq.core.domain.operation.notification.model.NotificationHistory;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 알림 발송 이력 응답 DTO.
 */
@Getter
@Builder
public class NotificationHistoryResponse {

    private Long id;
    private Long ruleId;
    private String channel;
    private String destination;
    private String message;
    private String status;
    private LocalDateTime sentAt;

    public static NotificationHistoryResponse from(NotificationHistory history) {
        return NotificationHistoryResponse.builder()
                .id(history.getId())
                .ruleId(history.getRuleId())
                .channel(history.getChannel().name())
                .destination(history.getDestination())
                .message(history.getMessage())
                .status(history.getStatus().name())
                .sentAt(history.getSentAt())
                .build();
    }
}
