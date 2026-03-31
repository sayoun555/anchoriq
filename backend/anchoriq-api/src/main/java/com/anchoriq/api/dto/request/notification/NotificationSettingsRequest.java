package com.anchoriq.api.dto.request.notification;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 알림 설정 수정 요청 DTO.
 */
@Getter
@NoArgsConstructor
public class NotificationSettingsRequest {

    private boolean slackEnabled;
    private String slackWebhookUrl;
    private boolean emailEnabled;
    private String emailAddress;
}
