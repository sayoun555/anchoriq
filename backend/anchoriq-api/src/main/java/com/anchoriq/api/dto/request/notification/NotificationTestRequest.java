package com.anchoriq.api.dto.request.notification;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 테스트 알림 발송 요청 DTO.
 */
@Getter
@NoArgsConstructor
public class NotificationTestRequest {

    @NotBlank(message = "Channel is required")
    private String channel;

    @NotBlank(message = "Destination is required")
    private String destination;

    private String message;
}
