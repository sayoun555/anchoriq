package com.anchoriq.api.application.port;

import com.anchoriq.core.common.exception.EntityNotFoundException;
import com.anchoriq.core.domain.maritime.port.model.Port;
import com.anchoriq.core.domain.maritime.port.repository.PortRepository;
import com.anchoriq.api.dto.response.port.PortResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 항만 Application Service 구현체.
 * 오케스트레이션만 수행한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PortApplicationServiceImpl implements PortApplicationService {

    private static final double CONGESTION_THRESHOLD = 70.0;

    private final PortRepository portRepository;
    private final StringRedisTemplate redisTemplate;

    @Override
    public List<PortResponse> findAll() {
        try {
            return portRepository.findAll().stream()
                    .map(PortResponse::from)
                    .toList();
        } catch (Exception e) {
            log.warn("Failed to fetch all ports: {}", e.getMessage());
            return List.of();
        }
    }

    @Override
    public List<PortResponse> findAll(int page, int size) {
        try {
            return portRepository.findAll(page, size).stream()
                    .map(PortResponse::from)
                    .toList();
        } catch (Exception e) {
            log.warn("Failed to fetch ports: {}", e.getMessage());
            return List.of();
        }
    }

    @Override
    public PortResponse findByLocode(String locode) {
        Port port = portRepository.findByLocode(locode)
                .orElseThrow(() -> new EntityNotFoundException("Port", locode));
        return PortResponse.from(port);
    }

    @Override
    public List<PortResponse> findByCountry(String country) {
        try {
            return portRepository.findByCountry(country).stream()
                    .map(PortResponse::from)
                    .toList();
        } catch (Exception e) {
            log.warn("Failed to fetch ports by country {}: {}", country, e.getMessage());
            return List.of();
        }
    }

    @Override
    public List<PortResponse> findCongestedPorts() {
        try {
            return portRepository.findCongestedPorts(CONGESTION_THRESHOLD).stream()
                    .map(PortResponse::from)
                    .toList();
        } catch (Exception e) {
            log.warn("Failed to fetch congested ports: {}", e.getMessage());
            return List.of();
        }
    }

    @Override
    public List<PortResponse> findTopCongestedPorts(int limit) {
        try {
            return portRepository.findTopCongestedPorts(limit).stream()
                    .map(PortResponse::from)
                    .toList();
        } catch (Exception e) {
            log.warn("Failed to fetch top congested ports: {}", e.getMessage());
            return List.of();
        }
    }

    @Override
    public double getCongestion(String locode) {
        Port port = portRepository.findByLocode(locode)
                .orElseThrow(() -> new EntityNotFoundException("Port", locode));
        return port.getCongestionValue();
    }

    @Override
    public Map<String, Object> getCongestionDetail(String locode) {
        Port port = portRepository.findByLocode(locode)
                .orElseThrow(() -> new EntityNotFoundException("Port", locode));

        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("locode", locode);
        detail.put("portName", port.getName());
        detail.put("congestionLevel", port.getCongestionValue());
        detail.put("congested", port.isCongested());
        detail.put("critical", port.isCriticalCongestion());

        try {
            String detailKey = "port:congestion:detail:" + locode;
            Map<Object, Object> cached = redisTemplate.opsForHash().entries(detailKey);
            if (!cached.isEmpty()) {
                detail.put("anchoredVessels", parseIntSafe(cached.get("anchoredVessels")));
                detail.put("mooredVessels", parseIntSafe(cached.get("mooredVessels")));
                detail.put("baselineRatio", parseDoubleSafe(cached.get("baselineRatio")));
                detail.put("severity", cached.getOrDefault("severity", "LOW"));
                detail.put("source", cached.getOrDefault("source", "UNKNOWN"));
                detail.put("calculatedAt", cached.getOrDefault("timestamp", ""));
            }
        } catch (Exception e) {
            // Tier 3: 캐시 조회 실패는 무시, 기본 정보만 반환
        }

        return detail;
    }

    @Override
    public long count() {
        try {
            return portRepository.count();
        } catch (Exception e) {
            log.warn("Failed to count ports: {}", e.getMessage());
            return 0;
        }
    }

    private int parseIntSafe(Object value) {
        if (value == null) return 0;
        try {
            return (int) Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private double parseDoubleSafe(Object value) {
        if (value == null) return 0.0;
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
}
