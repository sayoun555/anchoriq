package com.anchoriq.api.dto.response.notification;

import lombok.Builder;
import lombok.Getter;

/**
 * 알림 설정 응답 DTO.
 */
@Getter
@Builder
public class NotificationSettingsResponse {

    private boolean slackEnabled;
    private String slackWebhookUrl;
    private boolean emailEnabled;
    private String emailAddress;
    private int activeRuleCount;
}
