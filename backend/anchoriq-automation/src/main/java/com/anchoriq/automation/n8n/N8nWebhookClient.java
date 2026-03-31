package com.anchoriq.automation.n8n;

import com.anchoriq.core.event.RiskAlertEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Map;

/**
 * n8n 웹훅 HTTP 호출 구현체 — WebClient 기반.
 * Bean 등록은 AutomationConfig에서 수행한다.
 */
@Slf4j
public class N8nWebhookClient implements N8nClient {

    private final WebClient webClient;
    private final String riskAlertWebhookPath;

    public N8nWebhookClient(String baseUrl, String riskAlertWebhookPath) {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .build();
        this.riskAlertWebhookPath = riskAlertWebhookPath;
    }

    @Override
    public void triggerRiskAlert(RiskAlertEvent event) {
        Map<String, Object> payload = buildRiskAlertPayload(event);
        triggerWebhook(this.riskAlertWebhookPath, payload);
    }

    @Override
    public void triggerWebhook(String webhookPath, Object payload) {
        try {
            webClient.post()
                    .uri(webhookPath)
                    .bodyValue(payload)
                    .retrieve()
                    .toBodilessEntity()
                    .timeout(Duration.ofSeconds(10))
                    .doOnSuccess(response ->
                            log.info("n8n webhook triggered successfully: {}", webhookPath))
                    .doOnError(error ->
                            log.error("n8n webhook trigger failed: {} - {}", webhookPath, error.getMessage()))
                    .subscribe();
        } catch (Exception e) {
            log.error("Failed to trigger n8n webhook: {} - {}", webhookPath, e.getMessage());
        }
    }

    private Map<String, Object> buildRiskAlertPayload(RiskAlertEvent event) {
        return Map.of(
                "alertId", event.getAlertId(),
                "type", event.getType(),
                "riskLevel", event.getRiskLevel(),
                "vesselImo", event.getVesselImo() != null ? event.getVesselImo() : "",
                "vesselName", event.getVesselName() != null ? event.getVesselName() : "",
                "reason", event.getReason() != null ? event.getReason() : "",
                "recommendedAction", event.getRecommendedAction() != null ? event.getRecommendedAction() : "",
                "timestamp", event.getTimestamp().toString()
        );
    }
}
