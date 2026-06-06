package com.anchoriq.api.dto.response.notification;

import com.anchoriq.core.domain.operation.notification.model.NotificationSettings;
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

    /**
     * 저장된 설정 + 활성 규칙 수로 응답 생성.
     */
    public static NotificationSettingsResponse from(NotificationSettings settings, int activeRuleCount) {
        return NotificationSettingsResponse.builder()
                .slackEnabled(settings.isSlackEnabled())
                .slackWebhookUrl(settings.getSlackWebhookUrl())
                .emailEnabled(settings.isEmailEnabled())
                .emailAddress(settings.getEmailAddress())
                .activeRuleCount(activeRuleCount)
                .build();
    }

    /**
     * 설정이 아직 없는 유저 — 비활성 기본값.
     */
    public static NotificationSettingsResponse empty(int activeRuleCount) {
        return NotificationSettingsResponse.builder()
                .activeRuleCount(activeRuleCount)
                .build();
    }
}
