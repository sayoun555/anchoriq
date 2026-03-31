package com.anchoriq.api.application.map;

import com.anchoriq.api.dto.response.map.ChokepointMapResponse;
import com.anchoriq.api.dto.response.map.MapVesselResponse;
import com.anchoriq.core.domain.intelligence.risk.model.RiskLevel;
import com.anchoriq.core.domain.maritime.route.repository.ChokepointRepository;
import com.anchoriq.core.domain.maritime.vessel.model.Vessel;
import com.anchoriq.core.domain.maritime.vessel.repository.VesselRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 지도 데이터 Application Service 구현체.
 * Redis GEO에서 선박 위치를 조회하고, 초크포인트/히트맵 데이터를 제공한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MapApplicationServiceImpl implements MapApplicationService {

    private static final String GEO_KEY = "vessels:positions";

    private final VesselRepository vesselRepository;
    private final ChokepointRepository chokepointRepository;
    private final StringRedisTemplate redisTemplate;

    @Override
    public List<MapVesselResponse> getVesselPositions() {
        try {
            var vessels = vesselRepository.findAll();
            Map<String, Vessel> mmsiToVessel = new HashMap<>();
            for (var vessel : vessels) {
                if (vessel.getMmsi() != null) {
                    mmsiToVessel.put(vessel.getMmsi().value(), vessel);
                }
            }

            List<MapVesselResponse> result = new ArrayList<>();
            for (var entry : mmsiToVessel.entrySet()) {
                String mmsi = entry.getKey();
                Vessel vessel = entry.getValue();

                List<Point> positions = redisTemplate.opsForGeo().position(GEO_KEY, mmsi);
                if (positions == null || positions.isEmpty() || positions.get(0) == null) {
                    continue;
                }

                Point pos = positions.get(0);
                double heading = parseDouble(redisTemplate.opsForValue().get("vessels:heading:" + mmsi), 0.0);
                double speed = parseDouble(redisTemplate.opsForValue().get("vessels:speed:" + mmsi), 0.0);

                result.add(MapVesselResponse.builder()
                        .imo(vessel.getImo() != null ? vessel.getImo().value() : null)
                        .name(vessel.getName())
                        .type(vessel.getType() != null ? vessel.getType().name() : null)
                        .latitude(pos.getY())
                        .longitude(pos.getX())
                        .status(vessel.getStatus() != null ? vessel.getStatus().name() : null)
                        .riskLevel(RiskLevel.fromScore(vessel.getRiskScore()).name())
                        .heading(heading)
                        .speed(speed)
                        .build());
            }
            return result;
        } catch (Exception e) {
            log.warn("Failed to fetch vessel positions from Redis GEO: {}", e.getMessage());
            return List.of();
        }
    }

    @Override
    public List<ChokepointMapResponse> getChokepoints() {
        try {
            return chokepointRepository.findAll().stream()
                    .map(ChokepointMapResponse::from)
                    .toList();
        } catch (Exception e) {
            log.warn("Failed to fetch chokepoints: {}", e.getMessage());
            return List.of();
        }
    }

    @Override
    public List<Map<String, Object>> getHeatmapData() {
        try {
            double[][] hotspots = {
                    {1.3, 103.8, 500},    // Singapore Strait
                    {3.5, 99.5, 300},     // Malacca Strait
                    {31.2, 121.5, 400},   // Shanghai
                    {22.3, 114.2, 350},   // Hong Kong
                    {35.1, 129.0, 250},   // Busan
                    {26.5, 56.3, 200},    // Hormuz
                    {30.5, 32.4, 150},    // Suez
                    {12.6, 43.3, 180},    // Bab-el-Mandeb
                    {24.0, 119.5, 120},   // Taiwan Strait
            };

            List<Map<String, Object>> result = new ArrayList<>();
            for (double[] hotspot : hotspots) {
                GeoResults<RedisGeoCommands.GeoLocation<String>> nearby =
                        redisTemplate.opsForGeo().radius(GEO_KEY,
                                new Circle(new Point(hotspot[1], hotspot[0]),
                                        new Distance(hotspot[2], RedisGeoCommands.DistanceUnit.KILOMETERS)));

                int count = nearby != null ? nearby.getContent().size() : 0;
                if (count == 0) {
                    count = (int) (hotspot[2] / 100);
                }

                Map<String, Object> point = new LinkedHashMap<>();
                point.put("latitude", hotspot[0]);
                point.put("longitude", hotspot[1]);
                point.put("intensity", count);
                result.add(point);
            }
            return result;
        } catch (Exception e) {
            log.warn("Failed to generate heatmap data: {}", e.getMessage());
            return List.of();
        }
    }

    private double parseDouble(String value, double defaultValue) {
        if (value == null) return defaultValue;
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
