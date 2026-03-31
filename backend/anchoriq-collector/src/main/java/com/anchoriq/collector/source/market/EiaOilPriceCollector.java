package com.anchoriq.collector.source.market;

import com.anchoriq.collector.producer.MarketKafkaProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * EIA (Energy Information Administration) API 기반 유가 수집기.
 * WTI/Brent 유가를 수집하여 market-data 토픽으로 전송한다.
 * Bean 등록은 CollectorConfig에서 수행한다.
 */
public class EiaOilPriceCollector implements OilPriceCollector {

    private static final Logger log = LoggerFactory.getLogger(EiaOilPriceCollector.class);
    private static final String BASE_URL = "https://api.eia.gov/v2";
    private static final String WTI_SERIES = "PET.RWTC.D";
    private static final String BRENT_SERIES = "PET.RBRTE.D";

    private final WebClient webClient;
    private final MarketKafkaProducer marketKafkaProducer;
    private final String apiKey;

    public EiaOilPriceCollector(WebClient.Builder webClientBuilder,
                                MarketKafkaProducer marketKafkaProducer,
                                String apiKey) {
        this.webClient = webClientBuilder.baseUrl(BASE_URL).build();
        this.marketKafkaProducer = marketKafkaProducer;
        this.apiKey = apiKey;
    }

    @Override
    public void collect() {
        log.info("Starting oil price collection from EIA");
        try {
            Double wtiPrice = fetchLatestPrice(WTI_SERIES);
            Double brentPrice = fetchLatestPrice(BRENT_SERIES);

            Map<String, Object> indicators = new LinkedHashMap<>();
            if (wtiPrice != null) indicators.put("wti", wtiPrice);
            if (brentPrice != null) indicators.put("brent", brentPrice);

            if (indicators.isEmpty()) {
                log.warn("No oil price data available");
                return;
            }

            Map<String, Object> message = new LinkedHashMap<>();
            message.put("dataType", "OIL_PRICE");
            message.put("indicators", indicators);
            message.put("currency", "USD");
            message.put("timestamp", Instant.now().toString());

            marketKafkaProducer.send(null, message);
            log.info("Oil price data sent: WTI={}, Brent={}", wtiPrice, brentPrice);
        } catch (Exception e) {
            log.error("Failed to collect oil price data: {}", e.getMessage());
        }
    }

    @Override
    public String sourceName() {
        return "EIA";
    }

    @SuppressWarnings("unchecked")
    private Double fetchLatestPrice(String seriesId) {
        try {
            Map<String, Object> response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/petroleum/pri/spt/data/")
                            .queryParam("api_key", apiKey)
                            .queryParam("frequency", "daily")
                            .queryParam("data[0]", "value")
                            .queryParam("sort[0][column]", "period")
                            .queryParam("sort[0][direction]", "desc")
                            .queryParam("length", 1)
                            .build())
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response == null) return null;

            Map<String, Object> responseData = (Map<String, Object>) response.get("response");
            if (responseData == null) return null;

            List<Map<String, Object>> data = (List<Map<String, Object>>) responseData.get("data");
            if (data == null || data.isEmpty()) return null;

            Object value = data.get(0).get("value");
            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            }
            return null;
        } catch (Exception e) {
            log.warn("Failed to fetch price for series {}: {}", seriesId, e.getMessage());
            return null;
        }
    }
}
