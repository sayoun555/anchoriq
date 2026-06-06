package com.anchoriq.api.application.notification;

import com.anchoriq.api.dto.request.notification.NotificationRuleRequest;
import com.anchoriq.api.dto.request.notification.NotificationSettingsRequest;
import com.anchoriq.api.dto.request.notification.NotificationTestRequest;
import com.anchoriq.api.dto.response.notification.NotificationHistoryResponse;
import com.anchoriq.api.dto.response.notification.NotificationRuleResponse;
import com.anchoriq.api.dto.response.notification.NotificationSettingsResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * 알림 Application Service 인터페이스.
 */
public interface NotificationApplicationService {

    NotificationRuleResponse createRule(Long userId, NotificationRuleRequest request);

    Page<NotificationRuleResponse> getRules(Long userId, Pageable pageable);

    NotificationRuleResponse updateRule(Long userId, Long ruleId, NotificationRuleRequest request);

    void deleteRule(Long userId, Long ruleId);

    Page<NotificationHistoryResponse> getHistory(Pageable pageable);

    boolean sendTestNotification(NotificationTestRequest request);

    NotificationSettingsResponse getSettings(Long userId);

    NotificationSettingsResponse updateSettings(Long userId, NotificationSettingsRequest request);
}
