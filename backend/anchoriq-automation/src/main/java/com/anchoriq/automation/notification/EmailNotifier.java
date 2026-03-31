package com.anchoriq.automation.notification;

import com.anchoriq.core.domain.operation.notification.model.NotificationChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Map;

/**
 * 이메일 알림 발송 구현체.
 * n8n을 통해 이메일을 발송하는 방식을 사용한다 (외부 SMTP 직접 연동 대신).
 * n8n에 이메일 전용 웹훅 워크플로우가 설정되어 있어야 한다.
 * Bean 등록은 AutomationConfig에서 수행한다.
 */
@Slf4j
public class EmailNotifier implements Notifier {

    private final WebClient webClient;
    private final String emailWebhookUrl;

    public EmailNotifier(String n8nBaseUrl, String emailWebhookPath) {
        this.webClient = WebClient.builder().build();
        this.emailWebhookUrl = n8nBaseUrl + emailWebhookPath;
    }

    @Override
    public boolean send(String destination, String subject, String message) {
        try {
            Map<String, String> payload = Map.of(
                    "to", destination,
                    "subject", "[AnchorIQ] " + subject,
                    "body", message
            );

            webClient.post()
                    .uri(emailWebhookUrl)
                    .bodyValue(payload)
                    .retrieve()
                    .toBodilessEntity()
                    .timeout(Duration.ofSeconds(10))
                    .block();

            log.info("Email notification sent to: {}", maskEmail(destination));
            return true;
        } catch (Exception e) {
            log.error("Failed to send email notification to {}: {}", maskEmail(destination), e.getMessage());
            return false;
        }
    }

    @Override
    public NotificationChannel supportedChannel() {
        return NotificationChannel.EMAIL;
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "***";
        }
        int atIndex = email.indexOf('@');
        return email.substring(0, Math.min(2, atIndex)) + "***" + email.substring(atIndex);
    }
}
