package com.anchoriq.collector.source.news;

import com.anchoriq.collector.producer.NewsKafkaProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * GNews API 기반 해운 뉴스 수집기.
 * 해운 관련 키워드로 뉴스를 검색하여 news-events 토픽으로 전송한다.
 * Bean 등록은 CollectorConfig에서 수행한다.
 */
public class GNewsCollector implements NewsCollector {

    private static final Logger log = LoggerFactory.getLogger(GNewsCollector.class);
    private static final String BASE_URL = "https://gnews.io/api/v4";
    private static final String QUERY = "shipping OR maritime OR vessel OR port congestion OR chokepoint";

    private final WebClient webClient;
    private final NewsKafkaProducer newsKafkaProducer;
    private final String apiKey;

    public GNewsCollector(WebClient.Builder webClientBuilder,
                          NewsKafkaProducer newsKafkaProducer,
                          String apiKey) {
        this.webClient = webClientBuilder.baseUrl(BASE_URL).build();
        this.newsKafkaProducer = newsKafkaProducer;
        this.apiKey = apiKey;
    }

    @Override
    public void collect() {
        log.info("Starting news collection from GNews");
        try {
            Map<String, Object> response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/search")
                            .queryParam("q", QUERY)
                            .queryParam("lang", "en")
                            .queryParam("max", 10)
                            .queryParam("apikey", apiKey)
                            .build())
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response == null) {
                log.warn("No response from GNews");
                return;
            }

            List<Map<String, Object>> articles = extractArticles(response);
            int count = 0;
            for (Map<String, Object> article : articles) {
                Map<String, Object> message = toNewsMessage(article);
                newsKafkaProducer.send(null, message);
                count++;
            }
            log.info("News collection completed: {} articles sent", count);
        } catch (Exception e) {
            log.error("Failed to collect news: {}", e.getMessage());
        }
    }

    @Override
    public String sourceName() {
        return "GNews";
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractArticles(Map<String, Object> response) {
        Object articles = response.get("articles");
        if (articles instanceof List) {
            return (List<Map<String, Object>>) articles;
        }
        return List.of();
    }

    private Map<String, Object> toNewsMessage(Map<String, Object> article) {
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("title", article.getOrDefault("title", ""));
        message.put("source", extractSourceName(article));
        message.put("url", article.getOrDefault("url", ""));
        message.put("publishedAt", article.getOrDefault("publishedAt", ""));
        message.put("keywords", extractKeywords(article));
        message.put("timestamp", Instant.now().toString());
        return message;
    }

    @SuppressWarnings("unchecked")
    private String extractSourceName(Map<String, Object> article) {
        Object source = article.get("source");
        if (source instanceof Map) {
            return ((Map<String, Object>) source).getOrDefault("name", "Unknown").toString();
        }
        return "Unknown";
    }

    private List<String> extractKeywords(Map<String, Object> article) {
        String title = article.getOrDefault("title", "").toString().toLowerCase();
        return List.of("shipping", "maritime", "vessel").stream()
                .filter(title::contains)
                .toList();
    }
}
