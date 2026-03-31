package com.anchoriq.api.infrastructure.analytics;

import com.anchoriq.core.domain.analytics.model.TrendPoint;
import com.anchoriq.core.domain.analytics.service.MarketAnalyticsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 시장 데이터 분석 서비스 구현체.
 * Redis에 시계열 데이터를 저장하고 추세/상관관계를 계산한다.
 */
public class MarketAnalyticsServiceImpl implements MarketAnalyticsService {

    private static final Logger log = LoggerFactory.getLogger(MarketAnalyticsServiceImpl.class);

    private static final String OIL_PREFIX = "market:oil:";
    private static final String EXCHANGE_PREFIX = "market:exchange:";
    private static final Duration DATA_TTL = Duration.ofDays(365);

    private final StringRedisTemplate redisTemplate;

    public MarketAnalyticsServiceImpl(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public List<TrendPoint> getOilPriceTrend(String type, int days) {
        return getTrend(OIL_PREFIX + type.toLowerCase() + ":", days);
    }

    @Override
    public List<TrendPoint> getExchangeRateTrend(String pair, int days) {
        return getTrend(EXCHANGE_PREFIX + pair.toUpperCase() + ":", days);
    }

    @Override
    public double calculateCorrelation(int days) {
        List<TrendPoint> oilTrend = getOilPriceTrend("wti", days);
        List<TrendPoint> exchangeTrend = getExchangeRateTrend("USD-KRW", days);

        if (oilTrend.size() < 3 || exchangeTrend.size() < 3) {
            return 0.0;
        }

        return pearsonCorrelation(
                extractValues(oilTrend, days),
                extractValues(exchangeTrend, days));
    }

    @Override
    public void recordOilPrice(String type, double price) {
        try {
            String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
            String key = OIL_PREFIX + type.toLowerCase() + ":" + today;
            redisTemplate.opsForValue().set(key, String.valueOf(price), DATA_TTL);
        } catch (Exception e) {
            log.warn("Failed to record oil price: {}", e.getMessage());
        }
    }

    @Override
    public void recordExchangeRate(String pair, double rate) {
        try {
            String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
            String key = EXCHANGE_PREFIX + pair.toUpperCase() + ":" + today;
            redisTemplate.opsForValue().set(key, String.valueOf(rate), DATA_TTL);
        } catch (Exception e) {
            log.warn("Failed to record exchange rate: {}", e.getMessage());
        }
    }

    private List<TrendPoint> getTrend(String prefix, int days) {
        List<TrendPoint> trend = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            String key = prefix + date.format(DateTimeFormatter.ISO_LOCAL_DATE);
            try {
                String value = redisTemplate.opsForValue().get(key);
                if (value != null) {
                    trend.add(TrendPoint.of(date, Double.parseDouble(value)));
                }
            } catch (Exception e) {
                log.debug("Failed to get market data for {}: {}", key, e.getMessage());
            }
        }

        return trend;
    }

    private double[] extractValues(List<TrendPoint> points, int maxSize) {
        int size = Math.min(points.size(), maxSize);
        double[] values = new double[size];
        for (int i = 0; i < size; i++) {
            values[i] = points.get(i).value();
        }
        return values;
    }

    /**
     * 피어슨 상관계수를 계산한다.
     */
    private double pearsonCorrelation(double[] x, double[] y) {
        int n = Math.min(x.length, y.length);
        if (n < 2) return 0.0;

        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0, sumY2 = 0;
        for (int i = 0; i < n; i++) {
            sumX += x[i];
            sumY += y[i];
            sumXY += x[i] * y[i];
            sumX2 += x[i] * x[i];
            sumY2 += y[i] * y[i];
        }

        double numerator = n * sumXY - sumX * sumY;
        double denominator = Math.sqrt((n * sumX2 - sumX * sumX) * (n * sumY2 - sumY * sumY));

        if (denominator == 0.0) return 0.0;
        return numerator / denominator;
    }
}
