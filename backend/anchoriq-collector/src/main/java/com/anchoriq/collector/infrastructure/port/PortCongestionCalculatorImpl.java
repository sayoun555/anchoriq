package com.anchoriq.collector.infrastructure.port;

import com.anchoriq.core.common.exception.EntityNotFoundException;
import com.anchoriq.core.domain.maritime.port.model.CongestionReport;
import com.anchoriq.core.domain.maritime.port.model.Locode;
import com.anchoriq.core.domain.maritime.port.model.Port;
import com.anchoriq.core.domain.maritime.port.repository.PortRepository;
import com.anchoriq.core.domain.maritime.port.service.PortCongestionCalculator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * AIS + UNCTAD 이중 구조 항만 혼잡도 계산기 구현체.
 *
 * 계층 1 (실시간): Redis GEO에서 항만 반경 내 선박을 조회하여
 *   ANCHORED/MOORED 상태별로 분류하고 혼잡도 지수를 계산한다.
 * 계층 2 (기준선): Redis에 캐싱된 UNCTAD 기준선 대비 비율을 계산한다.
 *
 * Bean 등록은 CollectorConfig에서 수행한다.
 */
public class PortCongestionCalculatorImpl implements PortCongestionCalculator {

    private static final Logger log = LoggerFactory.getLogger(PortCongestionCalculatorImpl.class);

    private static final String GEO_KEY = "vessels:positions";
    private static final String VESSEL_STATUS_KEY_PREFIX = "vessels:status:";
    private static final String BASELINE_KEY_PREFIX = "baseline:";
    private static final double PORT_RADIUS_KM = 5.0;

    private final StringRedisTemplate redisTemplate;
    private final PortRepository portRepository;

    public PortCongestionCalculatorImpl(StringRedisTemplate redisTemplate,
                                         PortRepository portRepository) {
        this.redisTemplate = redisTemplate;
        this.portRepository = portRepository;
    }

    @Override
    public CongestionReport calculateCongestion(Locode locode) {
        Port port = portRepository.findByLocode(locode.value())
                .orElseThrow(() -> new EntityNotFoundException("Port", locode.value()));

        return calculateForPort(port);
    }

    @Override
    public List<CongestionReport> calculateAllPortsCongestion() {
        List<Port> ports = portRepository.findAll();
        List<CongestionReport> reports = new ArrayList<>();

        for (Port port : ports) {
            try {
                CongestionReport report = calculateForPort(port);
                reports.add(report);
            } catch (Exception e) {
                log.warn("Failed to calculate congestion for {}: {}",
                        port.getLocodeValue(), e.getMessage());
            }
        }

        log.info("Calculated congestion for {}/{} ports", reports.size(), ports.size());
        return reports;
    }

    private CongestionReport calculateForPort(Port port) {
        GeoResults<RedisGeoCommands.GeoLocation<String>> nearbyVessels = findNearbyVessels(port);

        int anchored = countByStatus(nearbyVessels, "ANCHORED");
        int moored = countByStatus(nearbyVessels, "MOORED");

        double congestionIndex = calculateIndex(anchored, moored);
        double baseline = getBaseline(port.getLocode());

        if (baseline > 0) {
            double ratio = (anchored + moored) / baseline;
            return CongestionReport.of(port.getLocode(), anchored, moored, congestionIndex, ratio);
        }

        return CongestionReport.withoutBaseline(port.getLocode(), anchored, moored, congestionIndex);
    }

    /**
     * Redis GEORADIUS로 항만 반경 내 선박을 조회한다.
     */
    private GeoResults<RedisGeoCommands.GeoLocation<String>> findNearbyVessels(Port port) {
        try {
            Point center = new Point(port.getLongitude(), port.getLatitude());
            Distance radius = new Distance(PORT_RADIUS_KM, Metrics.KILOMETERS);
            Circle searchArea = new Circle(center, radius);

            return redisTemplate.opsForGeo().radius(GEO_KEY, searchArea);
        } catch (Exception e) {
            log.warn("Redis GEO query failed for {}: {}", port.getLocodeValue(), e.getMessage());
            return null;
        }
    }

    /**
     * 조회된 선박 중 특정 상태의 선박 수를 카운트한다.
     * 각 선박의 상태는 vessels:status:{mmsi} 키에서 조회한다.
     */
    private int countByStatus(GeoResults<RedisGeoCommands.GeoLocation<String>> geoResults,
                               String targetStatus) {
        if (geoResults == null || geoResults.getContent().isEmpty()) {
            return 0;
        }

        int count = 0;
        for (var result : geoResults.getContent()) {
            String mmsi = result.getContent().getName();
            String status = getVesselStatus(mmsi);
            if (targetStatus.equals(status)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Redis에서 선박의 현재 상태를 조회한다.
     */
    private String getVesselStatus(String mmsi) {
        try {
            String key = VESSEL_STATUS_KEY_PREFIX + mmsi;
            String status = redisTemplate.opsForValue().get(key);
            return status != null ? status : "UNKNOWN";
        } catch (Exception e) {
            return "UNKNOWN";
        }
    }

    /**
     * 혼잡도 지수를 계산한다 (0~100).
     * 정박 대기 선박(ANCHORED)은 항만 혼잡의 주요 지표이므로 가중치 10,
     * 접안 선박(MOORED)은 정상 운영이므로 가중치 3.
     */
    private double calculateIndex(int anchored, int moored) {
        return Math.min(100.0, (anchored * 10.0) + (moored * 3.0));
    }

    /**
     * Redis에서 UNCTAD 기준선을 조회한다.
     */
    private double getBaseline(Locode locode) {
        try {
            String key = BASELINE_KEY_PREFIX + locode.value();
            String value = redisTemplate.opsForValue().get(key);
            return value != null ? Double.parseDouble(value) : 0.0;
        } catch (Exception e) {
            log.warn("Failed to read baseline for {}: {}", locode, e.getMessage());
            return 0.0;
        }
    }
}
