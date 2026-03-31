package com.anchoriq.collector.source.geopolitical;

import com.anchoriq.collector.producer.GeopoliticalKafkaProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * GDELT API 기반 지정학 이벤트 수집기.
 * 해운 관련 지정학 이벤트를 수집하여 geopolitical-events 토픽으로 전송한다.
 * Bean 등록은 CollectorConfig에서 수행한다.
 */
public class GdeltCollector implements GeopoliticalCollector {

    private static final Logger log = LoggerFactory.getLogger(GdeltCollector.class);
    private static final String BASE_URL = "https://api.gdeltproject.org/api/v2";
    private static final String QUERY = "maritime OR shipping OR naval OR strait OR chokepoint";

    private final WebClient webClient;
    private final GeopoliticalKafkaProducer geopoliticalKafkaProducer;

    public GdeltCollector(WebClient.Builder webClientBuilder,
                          GeopoliticalKafkaProducer geopoliticalKafkaProducer) {
        this.webClient = webClientBuilder.baseUrl(BASE_URL).build();
        this.geopoliticalKafkaProducer = geopoliticalKafkaProducer;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void collect() {
        log.info("Starting geopolitical event collection from GDELT");
        try {
            Map<String, Object> response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/doc/doc")
                            .queryParam("query", QUERY)
                            .queryParam("mode", "artlist")
                            .queryParam("maxrecords", 25)
                            .queryParam("format", "json")
                            .build())
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response == null) {
                log.warn("No response from GDELT");
                return;
            }

            List<Map<String, Object>> articles = (List<Map<String, Object>>) response.get("articles");
            if (articles == null) {
                log.warn("No articles in GDELT response");
                return;
            }

            int count = 0;
            for (Map<String, Object> article : articles) {
                Map<String, Object> message = toGeopoliticalMessage(article);
                geopoliticalKafkaProducer.send(null, message);
                count++;
            }
            log.info("Geopolitical event collection completed: {} events sent", count);
        } catch (Exception e) {
            log.error("Failed to collect geopolitical events: {}", e.getMessage());
        }
    }

    @Override
    public String sourceName() {
        return "GDELT";
    }

    private Map<String, Object> toGeopoliticalMessage(Map<String, Object> article) {
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("eventType", classifyEventType(article));
        message.put("region", extractRegion(article));
        message.put("lat", toDouble(article.get("sourcecountry_lat")));
        message.put("lon", toDouble(article.get("sourcecountry_lon")));
        message.put("severity", classifySeverity(article));
        message.put("description", article.getOrDefault("title", ""));
        message.put("source", "GDELT");
        message.put("url", article.getOrDefault("url", ""));
        message.put("timestamp", Instant.now().toString());
        return message;
    }

    private String classifyEventType(Map<String, Object> article) {
        String title = article.getOrDefault("title", "").toString().toLowerCase();
        if (title.contains("military") || title.contains("naval")) return "MILITARY_ACTIVITY";
        if (title.contains("attack") || title.contains("strike")) return "ARMED_CONFLICT";
        if (title.contains("sanction")) return "SANCTIONS";
        if (title.contains("piracy") || title.contains("pirate")) return "PIRACY";
        return "POLITICAL_TENSION";
    }

    private String extractRegion(Map<String, Object> article) {
        String title = article.getOrDefault("title", "").toString();
        if (title.contains("Hormuz")) return "Strait of Hormuz";
        if (title.contains("Malacca")) return "Strait of Malacca";
        if (title.contains("Suez")) return "Suez Canal";
        if (title.contains("Bab") || title.contains("Mandeb")) return "Bab el-Mandeb";
        if (title.contains("Taiwan")) return "Taiwan Strait";
        if (title.contains("Panama")) return "Panama Canal";
        return "Unknown";
    }

    private String classifySeverity(Map<String, Object> article) {
        String title = article.getOrDefault("title", "").toString().toLowerCase();
        if (title.contains("attack") || title.contains("strike") || title.contains("war")) return "HIGH";
        if (title.contains("tension") || title.contains("military")) return "MEDIUM";
        return "LOW";
    }

    private double toDouble(Object value) {
        if (value instanceof Number) return ((Number) value).doubleValue();
        return 0.0;
    }
}
