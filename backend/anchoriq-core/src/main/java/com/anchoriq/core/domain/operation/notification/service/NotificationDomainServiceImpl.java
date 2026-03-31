package com.anchoriq.core.domain.operation.notification.service;

import com.anchoriq.core.domain.operation.notification.model.NotificationRule;

import java.util.List;

/**
 * 알림 도메인 서비스 — 규칙 매칭 로직.
 * Bean 등록은 DomainServiceConfig에서 수행한다.
 */
public class NotificationDomainServiceImpl implements NotificationDomainService {

    @Override
    public List<NotificationRule> findMatchingRules(List<NotificationRule> activeRules,
                                                    String eventType, String riskLevel) {
        return activeRules.stream()
                .filter(rule -> rule.matches(eventType, riskLevel))
                .toList();
    }
}
