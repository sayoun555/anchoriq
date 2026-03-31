package com.anchoriq.automation.notification;

import com.anchoriq.core.domain.operation.notification.model.NotificationChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Map;

/**
 * Slack Webhook 알림 발송 구현체.
 * Bean 등록은 AutomationConfig에서 수행한다.
 */
@Slf4j
public class SlackNotifier implements Notifier {

    private final WebClient webClient;

    public SlackNotifier() {
        this.webClient = WebClient.builder().build();
    }

    @Override
    public boolean send(String destination, String subject, String message) {
        try {
            String slackMessage = formatSlackMessage(subject, message);

            webClient.post()
                    .uri(destination)
                    .bodyValue(Map.of("text", slackMessage))
                    .retrieve()
                    .toBodilessEntity()
                    .timeout(Duration.ofSeconds(10))
                    .block();

            log.info("Slack notification sent to: {}", maskWebhookUrl(destination));
            return true;
        } catch (Exception e) {
            log.error("Failed to send Slack notification: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public NotificationChannel supportedChannel() {
        return NotificationChannel.SLACK;
    }

    private String formatSlackMessage(String subject, String message) {
        return String.format(":warning: *[AnchorIQ Alert] %s*\n%s", subject, message);
    }

    private String maskWebhookUrl(String url) {
        if (url == null || url.length() < 20) {
            return "***";
        }
        return url.substring(0, 30) + "...";
    }
}
