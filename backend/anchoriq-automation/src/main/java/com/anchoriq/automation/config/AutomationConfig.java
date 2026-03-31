package com.anchoriq.automation.config;

import com.anchoriq.automation.n8n.N8nWebhookClient;
import com.anchoriq.automation.notification.EmailNotifier;
import com.anchoriq.automation.notification.NotificationDispatcherImpl;
import com.anchoriq.automation.notification.Notifier;
import com.anchoriq.automation.notification.SlackNotifier;
import com.anchoriq.automation.timeline.TimelineServiceImpl;
import com.anchoriq.core.domain.operation.notification.repository.NotificationHistoryRepository;
import com.anchoriq.core.domain.operation.notification.repository.NotificationRuleRepository;
import com.anchoriq.core.domain.operation.notification.service.NotificationDomainService;
import com.anchoriq.core.domain.operation.timeline.repository.TimelineRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Automation 모듈 Bean 설정.
 * n8n 연동, 알림 발송, 타임라인 서비스를 @Bean으로 명시적 등록한다.
 */
@Configuration
public class AutomationConfig {

    @Bean
    public N8nWebhookClient n8nWebhookClient(
            @Value("${n8n.base-url:http://localhost:5678}") String baseUrl,
            @Value("${n8n.webhook-path:/webhook/risk-alert}") String riskAlertWebhookPath) {
        return new N8nWebhookClient(baseUrl, riskAlertWebhookPath);
    }

    @Bean
    public SlackNotifier slackNotifier() {
        return new SlackNotifier();
    }

    @Bean
    public EmailNotifier emailNotifier(
            @Value("${n8n.base-url:http://localhost:5678}") String n8nBaseUrl,
            @Value("${n8n.email-webhook-path:/webhook/send-email}") String emailWebhookPath) {
        return new EmailNotifier(n8nBaseUrl, emailWebhookPath);
    }

    @Bean
    public NotificationDispatcherImpl notificationDispatcher(
            NotificationRuleRepository ruleRepository,
            NotificationHistoryRepository historyRepository,
            NotificationDomainService domainService,
            List<Notifier> notifiers) {
        return new NotificationDispatcherImpl(ruleRepository, historyRepository, domainService, notifiers);
    }

    @Bean
    @ConditionalOnBean(TimelineRepository.class)
    public TimelineServiceImpl timelineService(TimelineRepository timelineRepository) {
        return new TimelineServiceImpl(timelineRepository);
    }
}
