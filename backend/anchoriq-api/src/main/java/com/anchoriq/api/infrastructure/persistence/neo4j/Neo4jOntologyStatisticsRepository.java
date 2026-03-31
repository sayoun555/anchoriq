package com.anchoriq.api.infrastructure.persistence.neo4j;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;

/**
 * 온톨로지 통계 관련 Neo4j 쿼리 담당.
 */
@Component
@RequiredArgsConstructor
public class Neo4jOntologyStatisticsRepository {

    private final Driver driver;

    public Map<String, Long> getStatistics() {
        String cypher = """
                CALL {
                    MATCH (n) RETURN count(n) AS nodeCount
                }
                CALL {
                    MATCH ()-[r]->() RETURN count(r) AS relCount
                }
                RETURN nodeCount, relCount
                """;

        try (Session session = driver.session()) {
            return session.executeRead(tx -> {
                var result = tx.run(cypher);
                Map<String, Long> stats = new HashMap<>();
                if (result.hasNext()) {
                    Record record = result.next();
                    stats.put("totalNodes", record.get("nodeCount").asLong());
                    stats.put("totalRelationships", record.get("relCount").asLong());
                }
                return stats;
            });
        }
    }
}
