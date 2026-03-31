package com.anchoriq.api.infrastructure.persistence.neo4j;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Values;
import org.neo4j.driver.Session;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;

/**
 * 제재 네트워크 그래프 탐색 관련 Neo4j 쿼리 담당.
 */
@Component
@RequiredArgsConstructor
public class Neo4jSanctionNetworkRepository {

    private final Driver driver;

    public Map<String, Object> findSanctionNetwork() {
        String cypher = """
                MATCH (s:Sanction)<-[:SANCTIONED_BY]-(co:Country)<-[:HEADQUARTERED_IN]-(c:Company)<-[:OWNED_BY]-(v:Vessel)
                RETURN s, co, c, v
                LIMIT 200
                """;

        try (Session session = driver.session()) {
            return session.executeRead(tx -> {
                var result = tx.run(cypher);
                List<Map<String, Object>> nodes = new ArrayList<>();
                List<Map<String, Object>> relationships = new ArrayList<>();

                while (result.hasNext()) {
                    Record record = result.next();
                    nodes.add(Neo4jNodeHelper.toNodeMap(record.get("s").asNode()));
                    nodes.add(Neo4jNodeHelper.toNodeMap(record.get("co").asNode()));
                    nodes.add(Neo4jNodeHelper.toNodeMap(record.get("c").asNode()));
                    nodes.add(Neo4jNodeHelper.toNodeMap(record.get("v").asNode()));
                }

                Map<String, Object> network = new HashMap<>();
                network.put("nodes", nodes);
                network.put("relationships", relationships);
                return network;
            });
        }
    }

    public List<Map<String, Object>> findVesselsByCompany(String companyName) {
        String cypher = """
                MATCH (v:Vessel)-[:OWNED_BY]->(c:Company {name: $companyName})
                RETURN v
                """;

        try (Session session = driver.session()) {
            return session.executeRead(tx -> {
                var result = tx.run(cypher, Values.parameters("companyName", companyName));
                List<Map<String, Object>> vessels = new ArrayList<>();
                while (result.hasNext()) {
                    vessels.add(Neo4jNodeHelper.toNodeMap(result.next().get("v").asNode()));
                }
                return vessels;
            });
        }
    }

    public List<Map<String, Object>> findVesselsByCountry(String countryCode) {
        String cypher = """
                MATCH (v:Vessel)-[:OWNED_BY]->(c:Company)-[:HEADQUARTERED_IN]->(co:Country {isoCode: $code})
                RETURN v
                """;

        try (Session session = driver.session()) {
            return session.executeRead(tx -> {
                var result = tx.run(cypher, Values.parameters("code", countryCode));
                List<Map<String, Object>> vessels = new ArrayList<>();
                while (result.hasNext()) {
                    vessels.add(Neo4jNodeHelper.toNodeMap(result.next().get("v").asNode()));
                }
                return vessels;
            });
        }
    }
}
