package com.anchoriq.automation.notification;

import com.anchoriq.core.domain.operation.notification.model.NotificationChannel;

/**
 * 채널별 알림 발송 인터페이스.
 */
public interface Notifier {

    /**
     * 알림을 발송한다.
     *
     * @param destination 발송 대상 (Slack webhook URL 또는 이메일 주소)
     * @param subject     알림 제목
     * @param message     알림 내용
     * @return 발송 성공 여부
     */
    boolean send(String destination, String subject, String message);

    /**
     * 이 발송기가 처리하는 채널.
     */
    NotificationChannel supportedChannel();
}
