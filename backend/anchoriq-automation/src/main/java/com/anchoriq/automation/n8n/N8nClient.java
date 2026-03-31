package com.anchoriq.automation.n8n;

import com.anchoriq.core.event.RiskAlertEvent;

/**
 * n8n 웹훅 클라이언트 인터페이스.
 */
public interface N8nClient {

    /**
     * 리스크 알림 이벤트를 n8n 웹훅으로 전달하여 자동화 워크플로우를 트리거한다.
     */
    void triggerRiskAlert(RiskAlertEvent event);

    /**
     * 지정된 웹훅 URL로 커스텀 페이로드를 전달한다.
     */
    void triggerWebhook(String webhookPath, Object payload);
}
