package com.anchoriq.api.application.market;

import com.anchoriq.api.dto.response.market.MarketDataResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 시장 데이터 Application Service 구현체.
 * Redis에 캐싱된 유가/환율 데이터를 조회한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MarketApplicationServiceImpl implements MarketApplicationService {

    private final StringRedisTemplate redisTemplate;

    @Override
    public MarketDataResponse getOilPrice() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("wti", getRedisValue("market:oil:wti", "0.0"));
        data.put("brent", getRedisValue("market:oil:brent", "0.0"));
        return MarketDataResponse.builder()
                .type("oil_price")
                .data(data)
                .updatedAt(Instant.now().toString())
                .build();
    }

    @Override
    public MarketDataResponse getExchangeRate() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("usdKrw", getRedisValue("market:exchange:usd_krw", "0.0"));
        data.put("eurUsd", getRedisValue("market:exchange:eur_usd", "0.0"));
        return MarketDataResponse.builder()
                .type("exchange_rate")
                .data(data)
                .updatedAt(Instant.now().toString())
                .build();
    }

    @Override
    public MarketDataResponse getOverview() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("wti", getRedisValue("market:oil:wti", "0.0"));
        data.put("brent", getRedisValue("market:oil:brent", "0.0"));
        data.put("usdKrw", getRedisValue("market:exchange:usd_krw", "0.0"));
        data.put("eurUsd", getRedisValue("market:exchange:eur_usd", "0.0"));
        return MarketDataResponse.builder()
                .type("market_overview")
                .data(data)
                .updatedAt(Instant.now().toString())
                .build();
    }

    private String getRedisValue(String key, String defaultValue) {
        try {
            String value = redisTemplate.opsForValue().get(key);
            return value != null ? value : defaultValue;
        } catch (Exception e) {
            log.warn("Redis read failed for key={}, returning default: {}", key, e.getMessage());
            return defaultValue;
        }
    }
}
