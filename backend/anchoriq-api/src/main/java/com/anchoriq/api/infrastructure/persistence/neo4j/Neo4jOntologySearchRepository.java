package com.anchoriq.api.infrastructure.persistence.neo4j;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Values;
import org.neo4j.driver.Session;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;

/**
 * 온톨로지 엔티티 검색 관련 Neo4j 쿼리 담당.
 */
@Component
@RequiredArgsConstructor
public class Neo4jOntologySearchRepository {

    private final Driver driver;

    public List<Map<String, Object>> searchEntities(String query, int limit) {
        String cypher = """
                CALL {
                    MATCH (v:Vessel) WHERE toLower(v.name) CONTAINS toLower($query)
                        OR v.imo CONTAINS $query OR v.mmsi CONTAINS $query
                    RETURN v AS node, 'Vessel' AS label, v.name AS name
                    UNION
                    MATCH (p:Port) WHERE toLower(p.name) CONTAINS toLower($query)
                        OR p.locode CONTAINS toUpper($query)
                    RETURN p AS node, 'Port' AS label, p.name AS name
                    UNION
                    MATCH (c:Company) WHERE toLower(c.name) CONTAINS toLower($query)
                    RETURN c AS node, 'Company' AS label, c.name AS name
                    UNION
                    MATCH (co:Country) WHERE toLower(co.name) CONTAINS toLower($query)
                        OR co.isoCode = toUpper($query)
                    RETURN co AS node, 'Country' AS label, co.name AS name
                    UNION
                    MATCH (ch:Chokepoint) WHERE toLower(ch.name) CONTAINS toLower($query)
                        OR toLower(ch.displayName) CONTAINS toLower($query)
                    RETURN ch AS node, 'Chokepoint' AS label, ch.displayName AS name
                    UNION
                    MATCH (s:Sanction) WHERE toLower(s.caption) CONTAINS toLower($query)
                    RETURN s AS node, 'Sanction' AS label, s.caption AS name
                }
                RETURN node, label, name
                LIMIT $limit
                """;

        try (Session session = driver.session()) {
            return session.executeRead(tx -> {
                var result = tx.run(cypher, Values.parameters("query", query, "limit", limit));
                List<Map<String, Object>> entities = new ArrayList<>();

                while (result.hasNext()) {
                    Record record = result.next();
                    Map<String, Object> entity = Neo4jNodeHelper.toNodeMap(record.get("node").asNode());
                    entity.put("label", record.get("label").asString());
                    entities.add(entity);
                }
                return entities;
            });
        }
    }
}
