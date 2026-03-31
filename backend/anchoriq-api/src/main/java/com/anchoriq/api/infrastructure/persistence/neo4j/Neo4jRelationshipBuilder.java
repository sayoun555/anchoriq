package com.anchoriq.api.infrastructure.persistence.neo4j;

import lombok.RequiredArgsConstructor;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.Values;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResults;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Neo4j 온톨로지 관계 자동 생성 배치.
 * AIS 데이터 기반 Vessel과 다른 엔티티 간 관계를 주기적으로 생성한다.
 * - Vessel → Country (MMSI MID 기반 REGISTERED_IN)
 * - Vessel → Port (Redis GEO 반경 매칭 DOCKED_AT)
 * - Vessel → Chokepoint (Redis GEO 반경 매칭 TRANSITS)
 * - Vessel → Sanction (이름 매칭 SANCTIONED)
 * - Port → Country (LOCODE 기반 LOCATED_IN)
 */
@Component
@RequiredArgsConstructor
public class Neo4jRelationshipBuilder {

    private static final Logger log = LoggerFactory.getLogger(Neo4jRelationshipBuilder.class);
    private static final String GEO_KEY = "vessels:positions";

    private final Driver driver;
    private final StringRedisTemplate redisTemplate;

    /**
     * 5분마다 실행. 모든 관계를 일괄 생성/갱신한다.
     */
    @Scheduled(fixedDelay = 300_000, initialDelay = 30_000)
    public void buildRelationships() {
        log.info("Starting relationship build batch...");
        int total = 0;
        total += buildVesselCountryRelations();
        total += buildPortCountryRelations();
        total += buildVesselPortRelations();
        total += buildVesselChokepointRelations();
        total += buildVesselSanctionRelations();
        log.info("Relationship build completed: {} relationships created/updated", total);
    }

    /**
     * Vessel → Country (REGISTERED_IN)
     * MMSI 앞 3자리 MID로 국가 매칭.
     */
    private int buildVesselCountryRelations() {
        String cypher = """
                MATCH (v:Vessel), (c:Country)
                WHERE v.mmsi IS NOT NULL AND size(v.mmsi) >= 3
                  AND v.flag IS NOT NULL AND v.flag <> ''
                  AND c.isoCode = v.flag
                MERGE (v)-[:REGISTERED_IN]->(c)
                RETURN count(*) AS cnt
                """;
        return executeCount(cypher, "Vessel→Country REGISTERED_IN");
    }

    /**
     * Port → Country (LOCATED_IN)
     * LOCODE 앞 2자리 = 국가코드.
     */
    private int buildPortCountryRelations() {
        String cypher = """
                MATCH (p:Port), (c:Country)
                WHERE p.locode IS NOT NULL AND size(p.locode) >= 2
                  AND c.isoCode = substring(p.locode, 0, 2)
                MERGE (p)-[:LOCATED_IN]->(c)
                RETURN count(*) AS cnt
                """;
        return executeCount(cypher, "Port→Country LOCATED_IN");
    }

    /**
     * Vessel → Port (DOCKED_AT)
     * Redis GEO에서 항만 반경 5km 내 정박/정류 중인 선박 매칭.
     * ANCHORED 또는 MOORED 상태만.
     */
    private int buildVesselPortRelations() {
        String findPorts = "MATCH (p:Port) WHERE p.latitude IS NOT NULL AND p.longitude IS NOT NULL RETURN p.name AS name, p.latitude AS lat, p.longitude AS lon, p.locode AS locode";
        int count = 0;

        try (Session session = driver.session()) {
            var ports = session.executeRead(tx -> {
                var result = tx.run(findPorts);
                return result.list(r -> Map.of(
                        "name", r.get("name").asString(""),
                        "lat", r.get("lat").asDouble(0),
                        "lon", r.get("lon").asDouble(0),
                        "locode", r.get("locode").asString("")
                ));
            });

            for (var port : ports) {
                double lat = (double) port.get("lat");
                double lon = (double) port.get("lon");
                String locode = (String) port.get("locode");
                if (lat == 0 && lon == 0) continue;

                GeoResults<RedisGeoCommands.GeoLocation<String>> nearby = redisTemplate.opsForGeo()
                        .radius(GEO_KEY, new Circle(new Point(lon, lat),
                                new Distance(5, RedisGeoCommands.DistanceUnit.KILOMETERS)));

                if (nearby == null || nearby.getContent().isEmpty()) continue;

                List<String> mmsiList = nearby.getContent().stream()
                        .map(r -> r.getContent().getName())
                        .collect(Collectors.toList());

                String cypher = """
                        UNWIND $mmsiList AS mmsi
                        MATCH (v:Vessel {mmsi: mmsi}), (p:Port {locode: $locode})
                        MERGE (v)-[:DOCKED_AT]->(p)
                        RETURN count(*) AS cnt
                        """;
                try {
                    var result = session.executeWrite(tx ->
                            tx.run(cypher, Values.parameters("mmsiList", mmsiList, "locode", locode))
                                    .single().get("cnt").asInt());
                    count += result;
                } catch (Exception e) {
                    log.debug("Port {} relation build skipped: {}", locode, e.getMessage());
                }
            }
        } catch (Exception e) {
            log.warn("Vessel→Port relation build failed: {}", e.getMessage());
        }

        log.info("  Vessel→Port DOCKED_AT: {}", count);
        return count;
    }

    /**
     * Vessel → Chokepoint (TRANSITS)
     * Redis GEO에서 해협 반경 100km 내 항행 중인 선박 매칭.
     */
    private int buildVesselChokepointRelations() {
        String findChokepoints = "MATCH (ch:Chokepoint) RETURN ch.name AS name, ch.lat AS lat, ch.lon AS lon";
        int count = 0;

        try (Session session = driver.session()) {
            var chokepoints = session.executeRead(tx -> {
                var result = tx.run(findChokepoints);
                return result.list(r -> Map.of(
                        "name", r.get("name").asString(""),
                        "lat", r.get("lat").asDouble(0),
                        "lon", r.get("lon").asDouble(0)
                ));
            });

            for (var cp : chokepoints) {
                double lat = (double) cp.get("lat");
                double lon = (double) cp.get("lon");
                String name = (String) cp.get("name");

                GeoResults<RedisGeoCommands.GeoLocation<String>> nearby = redisTemplate.opsForGeo()
                        .radius(GEO_KEY, new Circle(new Point(lon, lat),
                                new Distance(100, RedisGeoCommands.DistanceUnit.KILOMETERS)));

                if (nearby == null || nearby.getContent().isEmpty()) continue;

                List<String> mmsiList = nearby.getContent().stream()
                        .map(r -> r.getContent().getName())
                        .collect(Collectors.toList());

                String cypher = """
                        UNWIND $mmsiList AS mmsi
                        MATCH (v:Vessel {mmsi: mmsi}), (ch:Chokepoint {name: $name})
                        MERGE (v)-[:TRANSITS]->(ch)
                        RETURN count(*) AS cnt
                        """;
                try {
                    var result = session.executeWrite(tx ->
                            tx.run(cypher, Values.parameters("mmsiList", mmsiList, "name", name))
                                    .single().get("cnt").asInt());
                    count += result;
                } catch (Exception e) {
                    log.debug("Chokepoint {} relation build skipped: {}", name, e.getMessage());
                }
            }
        } catch (Exception e) {
            log.warn("Vessel→Chokepoint relation build failed: {}", e.getMessage());
        }

        log.info("  Vessel→Chokepoint TRANSITS: {}", count);
        return count;
    }

    /**
     * Vessel → Sanction (SANCTIONED)
     * 제재 대상 이름과 선박 이름 매칭.
     */
    private int buildVesselSanctionRelations() {
        String cypher = """
                MATCH (v:Vessel), (s:Sanction)
                WHERE s.targetType = 'VESSEL'
                  AND toLower(v.name) = toLower(s.caption)
                MERGE (v)-[:SANCTIONED]->(s)
                RETURN count(*) AS cnt
                """;
        return executeCount(cypher, "Vessel→Sanction SANCTIONED");
    }

    private int executeCount(String cypher, String label) {
        try (Session session = driver.session()) {
            int count = session.executeWrite(tx ->
                    tx.run(cypher).single().get("cnt").asInt());
            log.info("  {}: {}", label, count);
            return count;
        } catch (Exception e) {
            log.warn("  {} failed: {}", label, e.getMessage());
            return 0;
        }
    }
}
