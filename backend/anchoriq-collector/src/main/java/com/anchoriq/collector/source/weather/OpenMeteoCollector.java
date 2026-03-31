package com.anchoriq.collector.source.weather;

import com.anchoriq.collector.common.CollectorException;
import com.anchoriq.collector.producer.WeatherKafkaProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Open-Meteo API 기반 기상 데이터 수집기.
 * 주요 해역의 기상 정보를 수집하여 weather-events 토픽으로 전송한다.
 * Bean 등록은 CollectorConfig에서 수행한다.
 */
public class OpenMeteoCollector implements WeatherCollector {

    private static final Logger log = LoggerFactory.getLogger(OpenMeteoCollector.class);
    private static final String BASE_URL = "https://marine-api.open-meteo.com/v1";

    private static final List<SeaZoneCoordinate> SEA_ZONES = List.of(
            new SeaZoneCoordinate("East China Sea", 30.0, 125.0),
            new SeaZoneCoordinate("South China Sea", 15.0, 115.0),
            new SeaZoneCoordinate("Strait of Malacca", 2.0, 101.0),
            new SeaZoneCoordinate("Arabian Sea", 15.0, 65.0),
            new SeaZoneCoordinate("Red Sea", 20.0, 38.0),
            new SeaZoneCoordinate("Gulf of Aden", 12.0, 45.0),
            new SeaZoneCoordinate("Persian Gulf", 26.0, 52.0),
            new SeaZoneCoordinate("Bay of Bengal", 15.0, 85.0),
            new SeaZoneCoordinate("Sea of Japan", 40.0, 135.0),
            new SeaZoneCoordinate("Taiwan Strait", 24.0, 119.0)
    );

    private final WebClient webClient;
    private final WeatherKafkaProducer weatherKafkaProducer;

    public OpenMeteoCollector(WebClient.Builder webClientBuilder,
                              WeatherKafkaProducer weatherKafkaProducer) {
        this.webClient = webClientBuilder.baseUrl(BASE_URL).build();
        this.weatherKafkaProducer = weatherKafkaProducer;
    }

    @Override
    public void collect() {
        log.info("Starting weather data collection from Open-Meteo");
        for (SeaZoneCoordinate zone : SEA_ZONES) {
            try {
                collectForZone(zone);
            } catch (Exception e) {
                log.error("Failed to collect weather for zone {}: {}", zone.name, e.getMessage());
            }
        }
        log.info("Weather data collection completed");
    }

    @Override
    public String sourceName() {
        return "Open-Meteo";
    }

    private void collectForZone(SeaZoneCoordinate zone) {
        Map<String, Object> response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/marine")
                        .queryParam("latitude", zone.lat)
                        .queryParam("longitude", zone.lon)
                        .queryParam("current", "wave_height,wind_speed_10m,wind_direction_10m")
                        .build())
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (response == null) {
            return;
        }

        Map<String, Object> current = (Map<String, Object>) response.get("current");
        if (current == null) {
            return;
        }

        Map<String, Object> message = new LinkedHashMap<>();
        message.put("zone", zone.name);
        message.put("lat", zone.lat);
        message.put("lon", zone.lon);
        message.put("type", classifyWeatherType(current));
        message.put("severity", classifySeverity(current));
        message.put("windSpeed", current.get("wind_speed_10m"));
        message.put("waveHeight", current.get("wave_height"));
        message.put("timestamp", Instant.now().toString());

        weatherKafkaProducer.send(null, message);
    }

    private String classifyWeatherType(Map<String, Object> current) {
        double windSpeed = toDouble(current.get("wind_speed_10m"));
        double waveHeight = toDouble(current.get("wave_height"));

        if (windSpeed >= 33.0) return "TYPHOON";
        if (windSpeed >= 20.0 || waveHeight >= 4.0) return "STORM";
        if (windSpeed >= 10.0) return "HIGH_WIND";
        return "CLEAR";
    }

    private String classifySeverity(Map<String, Object> current) {
        double windSpeed = toDouble(current.get("wind_speed_10m"));
        if (windSpeed >= 33.0) return "CRITICAL";
        if (windSpeed >= 20.0) return "HIGH";
        if (windSpeed >= 10.0) return "MEDIUM";
        return "LOW";
    }

    private double toDouble(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return 0.0;
    }

    private record SeaZoneCoordinate(String name, double lat, double lon) {
    }
}
