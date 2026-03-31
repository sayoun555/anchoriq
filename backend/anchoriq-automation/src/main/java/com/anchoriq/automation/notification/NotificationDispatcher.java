package com.anchoriq.automation.notification;

import com.anchoriq.core.event.RiskAlertEvent;

/**
 * 알림 디스패처 인터페이스 — 규칙 매칭 후 적절한 채널로 발송.
 */
public interface NotificationDispatcher {

    /**
     * 리스크 이벤트를 모든 매칭 규칙에 따라 발송.
     */
    void dispatch(RiskAlertEvent event);

    /**
     * 테스트 알림 발송.
     */
    boolean sendTest(String channel, String destination, String message);
}
