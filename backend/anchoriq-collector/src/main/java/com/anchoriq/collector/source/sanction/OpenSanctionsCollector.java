package com.anchoriq.collector.source.sanction;

import com.anchoriq.collector.producer.SanctionKafkaProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.BufferedReader;
import java.io.StringReader;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * OpenSanctions JSON API 기반 제재 데이터 수집기.
 * UN 제재 목록을 수집하여 sanction-updates 토픽으로 전송한다.
 * Bean 등록은 CollectorConfig에서 수행한다.
 */
public class OpenSanctionsCollector implements SanctionCollector {

    private static final Logger log = LoggerFactory.getLogger(OpenSanctionsCollector.class);
    private static final String BASE_URL = "https://data.opensanctions.org";

    private final WebClient webClient;
    private final SanctionKafkaProducer sanctionKafkaProducer;

    public OpenSanctionsCollector(WebClient.Builder webClientBuilder,
                                  SanctionKafkaProducer sanctionKafkaProducer) {
        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(configurer -> configurer.defaultCodecs()
                        .maxInMemorySize(10 * 1024 * 1024))
                .build();
        this.webClient = webClientBuilder
                .baseUrl(BASE_URL)
                .exchangeStrategies(strategies)
                .build();
        this.sanctionKafkaProducer = sanctionKafkaProducer;
    }

    @Override
    public void collect() {
        log.info("Starting sanction data collection from OpenSanctions (NDJSON)");
        try {
            String ndjson = webClient.get()
                    .uri("/datasets/latest/un_sc_sanctions/entities.ftm.json")
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (ndjson == null || ndjson.isBlank()) {
                log.warn("No response from OpenSanctions entities endpoint");
                return;
            }

            List<Map<String, Object>> entities = parseNdjson(ndjson);
            int count = 0;
            for (Map<String, Object> entity : entities) {
                String schema = (String) entity.getOrDefault("schema", "");
                if ("Vessel".equals(schema) || "Organization".equals(schema)
                        || "Company".equals(schema) || "Person".equals(schema)
                        || "LegalEntity".equals(schema) || "Sanction".equals(schema)) {
                    Map<String, Object> message = toSanctionMessage(entity);
                    if (message != null) {
                        sanctionKafkaProducer.send(null, message);
                        count++;
                    }
                }
            }
            log.info("Sanction data collection completed: {} entities parsed, {} targets sent", entities.size(), count);
        } catch (Exception e) {
            log.error("Failed to collect sanction data: {}", e.getMessage());
        }
    }

    @Override
    public String sourceName() {
        return "OpenSanctions";
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> parseNdjson(String ndjson) {
        List<Map<String, Object>> entities = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new StringReader(ndjson))) {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.isBlank()) {
                    entities.add(mapper.readValue(line, Map.class));
                }
            }
        } catch (Exception e) {
            log.error("Failed to parse NDJSON: {}", e.getMessage());
        }
        return entities;
    }

    private Map<String, Object> toSanctionMessage(Map<String, Object> entity) {
        try {
            Map<String, Object> message = new LinkedHashMap<>();
            message.put("action", "UPDATED");
            message.put("targetType", resolveTargetType(entity));
            message.put("targetName", entity.getOrDefault("caption", "UNKNOWN"));
            message.put("targetImo", extractProperty(entity, "imoNumber"));
            message.put("referenceNumber", entity.getOrDefault("id", ""));
            message.put("reason", extractProperty(entity, "notes"));
            message.put("country", extractProperty(entity, "country"));
            message.put("timestamp", Instant.now().toString());
            return message;
        } catch (Exception e) {
            log.warn("Failed to parse sanction entity: {}", e.getMessage());
            return null;
        }
    }

    private String resolveTargetType(Map<String, Object> entity) {
        Object schema = entity.get("schema");
        if ("Vessel".equals(schema)) return "VESSEL";
        if ("Organization".equals(schema) || "Company".equals(schema) || "LegalEntity".equals(schema)) return "COMPANY";
        if ("Person".equals(schema)) return "PERSON";
        if ("Sanction".equals(schema)) return "SANCTION";
        return "OTHER";
    }

    @SuppressWarnings("unchecked")
    private String extractProperty(Map<String, Object> entity, String propertyName) {
        Object properties = entity.get("properties");
        if (properties instanceof Map) {
            Map<String, Object> props = (Map<String, Object>) properties;
            Object value = props.get(propertyName);
            if (value instanceof List && !((List<?>) value).isEmpty()) {
                return ((List<?>) value).get(0).toString();
            }
            if (value != null) {
                return value.toString();
            }
        }
        return "";
    }
}
