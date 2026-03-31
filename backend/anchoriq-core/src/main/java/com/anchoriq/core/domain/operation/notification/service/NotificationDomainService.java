package com.anchoriq.core.domain.operation.notification.service;

import com.anchoriq.core.domain.operation.notification.model.NotificationRule;

import java.util.List;

/**
 * 알림 도메인 서비스 인터페이스 — 규칙 매칭, 채널별 발송 결정.
 */
public interface NotificationDomainService {

    /**
     * 활성 규칙 중 주어진 이벤트에 매칭되는 규칙 목록 반환.
     */
    List<NotificationRule> findMatchingRules(List<NotificationRule> activeRules,
                                             String eventType, String riskLevel);
}
