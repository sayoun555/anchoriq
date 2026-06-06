package com.anchoriq.automation.notification;

import com.anchoriq.core.domain.operation.notification.model.NotificationChannel;
import com.anchoriq.core.domain.operation.notification.model.NotificationHistory;
import com.anchoriq.core.domain.operation.notification.model.NotificationRule;
import com.anchoriq.core.domain.operation.notification.repository.NotificationHistoryRepository;
import com.anchoriq.core.domain.operation.notification.repository.NotificationRuleRepository;
import com.anchoriq.core.domain.operation.notification.repository.NotificationSettingsRepository;
import com.anchoriq.core.domain.operation.notification.service.NotificationDomainService;
import com.anchoriq.core.event.RiskAlertEvent;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 알림 디스패처 구현체 — 규칙 매칭 후 채널별 Notifier로 발송, 이력 저장.
 * Bean 등록은 AutomationConfig에서 수행한다.
 */
@Slf4j
public class NotificationDispatcherImpl implements NotificationDispatcher {

    private final NotificationRuleRepository ruleRepository;
    private final NotificationHistoryRepository historyRepository;
    private final NotificationSettingsRepository settingsRepository;
    private final NotificationDomainService domainService;
    private final Map<NotificationChannel, Notifier> notifierMap;

    public NotificationDispatcherImpl(NotificationRuleRepository ruleRepository,
                                      NotificationHistoryRepository historyRepository,
                                      NotificationSettingsRepository settingsRepository,
                                      NotificationDomainService domainService,
                                      List<Notifier> notifiers) {
        this.ruleRepository = ruleRepository;
        this.historyRepository = historyRepository;
        this.settingsRepository = settingsRepository;
        this.domainService = domainService;
        this.notifierMap = notifiers.stream()
                .collect(Collectors.toMap(Notifier::supportedChannel, Function.identity()));
    }

    @Override
    public void dispatch(RiskAlertEvent event) {
        List<NotificationRule> activeRules = ruleRepository.findActiveRules();
        List<NotificationRule> matchingRules = domainService.findMatchingRules(
                activeRules, event.getType(), event.getRiskLevel());

        if (matchingRules.isEmpty()) {
            log.debug("No matching notification rules for event: {}", event.getAlertId());
            return;
        }

        String subject = buildSubject(event);
        String message = buildMessage(event);

        for (NotificationRule rule : matchingRules) {
            sendAndRecord(rule, subject, message);
        }
    }

    @Override
    public boolean sendTest(String channel, String destination, String message) {
        NotificationChannel notificationChannel = NotificationChannel.valueOf(channel.toUpperCase());
        Notifier notifier = notifierMap.get(notificationChannel);
        if (notifier == null) {
            log.error("No notifier found for channel: {}", channel);
            return false;
        }
        return notifier.send(destination, "Test Notification", message);
    }

    private void sendAndRecord(NotificationRule rule, String subject, String message) {
        Notifier notifier = notifierMap.get(rule.getChannel());
        if (notifier == null) {
            log.error("No notifier available for channel: {}", rule.getChannel());
            return;
        }

        String destination = resolveDestination(rule);
        if (destination == null) {
            log.warn("No {} destination configured for user {} (rule {}). Skipping notification.",
                    rule.getChannel(), rule.getUserId(), rule.getId());
            return;
        }

        boolean success = notifier.send(destination, subject, message);

        NotificationHistory history = success
                ? NotificationHistory.recordSent(rule.getId(), rule.getChannel(), destination, message)
                : NotificationHistory.recordFailed(rule.getId(), rule.getChannel(), destination, message);

        historyRepository.save(history);
    }

    /**
     * 규칙 소유자의 알림 설정에서 채널별 실제 발송 대상(Slack 웹훅 URL / 이메일 주소)을 조회한다.
     * 설정이 없거나 해당 채널이 비활성·미설정이면 null을 반환해 발송을 생략한다.
     */
    private String resolveDestination(NotificationRule rule) {
        return settingsRepository.findByUserId(rule.getUserId())
                .filter(settings -> settings.hasDestinationFor(rule.getChannel()))
                .map(settings -> settings.destinationFor(rule.getChannel()))
                .orElse(null);
    }

    private String buildSubject(RiskAlertEvent event) {
        return String.format("[%s] %s", event.getRiskLevel(), event.getType());
    }

    private String buildMessage(RiskAlertEvent event) {
        StringBuilder sb = new StringBuilder();
        sb.append("Risk Alert: ").append(event.getType()).append("\n");
        sb.append("Level: ").append(event.getRiskLevel()).append("\n");
        if (event.getVesselName() != null) {
            sb.append("Vessel: ").append(event.getVesselName())
                    .append(" (IMO: ").append(event.getVesselImo()).append(")\n");
        }
        if (event.getReason() != null) {
            sb.append("Reason: ").append(event.getReason()).append("\n");
        }
        if (event.getRecommendedAction() != null) {
            sb.append("Recommended: ").append(event.getRecommendedAction()).append("\n");
        }
        return sb.toString();
    }
}
