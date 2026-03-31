package com.anchoriq.collector.source.market;

import com.anchoriq.collector.producer.MarketKafkaProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Frankfurter.app API 기반 환율 수집기.
 * USD 기준 주요 통화 환율을 수집하여 market-data 토픽으로 전송한다.
 * Bean 등록은 CollectorConfig에서 수행한다.
 */
public class FrankfurterCollector implements ExchangeRateCollector {

    private static final Logger log = LoggerFactory.getLogger(FrankfurterCollector.class);
    private static final String BASE_URL = "https://api.frankfurter.app";

    private final WebClient webClient;
    private final MarketKafkaProducer marketKafkaProducer;

    public FrankfurterCollector(WebClient.Builder webClientBuilder,
                                MarketKafkaProducer marketKafkaProducer) {
        this.webClient = webClientBuilder.baseUrl(BASE_URL).build();
        this.marketKafkaProducer = marketKafkaProducer;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void collect() {
        log.info("Starting exchange rate collection from Frankfurter");
        try {
            Map<String, Object> response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/latest")
                            .queryParam("from", "USD")
                            .queryParam("to", "KRW,EUR,JPY,CNY,GBP")
                            .build())
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response == null) {
                log.warn("No response from Frankfurter");
                return;
            }

            Map<String, Object> rates = (Map<String, Object>) response.get("rates");
            if (rates == null || rates.isEmpty()) {
                log.warn("No exchange rate data available");
                return;
            }

            Map<String, Object> message = new LinkedHashMap<>();
            message.put("dataType", "EXCHANGE_RATE");
            message.put("base", "USD");
            message.put("rates", rates);
            message.put("timestamp", Instant.now().toString());

            marketKafkaProducer.send(null, message);
            log.info("Exchange rate data sent: {}", rates);
        } catch (Exception e) {
            log.error("Failed to collect exchange rate data: {}", e.getMessage());
        }
    }

    @Override
    public String sourceName() {
        return "Frankfurter";
    }
}
