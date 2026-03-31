package com.anchoriq.api.infrastructure.persistence.neo4j;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Values;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Relationship;
import org.neo4j.driver.Session;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;

/**
 * 노드 확장(expandNode) 및 그래프 개요(graphOverview) Neo4j 쿼리 담당.
 */
@Component
@RequiredArgsConstructor
public class Neo4jGraphExpansionRepository {

    private final Driver driver;

    public Map<String, Object> expandNode(Long nodeId, int depth) {
        String cypher = """
                MATCH (n) WHERE id(n) = $nodeId
                OPTIONAL MATCH (n)-[r]-(m)
                WITH n, collect(DISTINCT m)[0..50] AS connectedNodes, collect(DISTINCT r)[0..50] AS rels
                RETURN n, connectedNodes, rels
                """;

        try (Session session = driver.session()) {
            return session.executeRead(tx -> {
                var result = tx.run(cypher, Values.parameters("nodeId", nodeId));
                Map<String, Object> graphData = new HashMap<>();
                List<Map<String, Object>> nodes = new ArrayList<>();
                List<Map<String, Object>> relationships = new ArrayList<>();

                while (result.hasNext()) {
                    Record record = result.next();
                    Node centerNode = record.get("n").asNode();
                    nodes.add(Neo4jNodeHelper.toNodeMap(centerNode));

                    record.get("connectedNodes").asList().forEach(cn -> {
                        if (cn instanceof Node node) {
                            nodes.add(Neo4jNodeHelper.toNodeMap(node));
                        }
                    });

                    record.get("rels").asList().forEach(r -> {
                        if (r instanceof Relationship rel) {
                            relationships.add(Neo4jNodeHelper.toRelMap(rel));
                        }
                    });
                }

                graphData.put("nodes", nodes);
                graphData.put("relationships", relationships);
                return graphData;
            });
        }
    }

    public Map<String, Object> getGraphOverview(int nodeLimit) {
        String cypher = """
                MATCH (n)
                WITH n LIMIT $limit
                OPTIONAL MATCH (n)-[r]->(m)
                RETURN collect(DISTINCT n) + collect(DISTINCT m) AS nodes,
                       collect(DISTINCT r) AS relationships
                """;

        try (Session session = driver.session()) {
            return session.executeRead(tx -> {
                var result = tx.run(cypher, Values.parameters("limit", nodeLimit));
                Map<String, Object> graphData = new HashMap<>();
                List<Map<String, Object>> nodes = new ArrayList<>();
                List<Map<String, Object>> rels = new ArrayList<>();

                if (result.hasNext()) {
                    Record record = result.next();

                    record.get("nodes").asList().forEach(n -> {
                        if (n instanceof Node node) {
                            nodes.add(Neo4jNodeHelper.toNodeMap(node));
                        }
                    });

                    record.get("relationships").asList().forEach(r -> {
                        if (r instanceof Relationship rel) {
                            rels.add(Neo4jNodeHelper.toRelMap(rel));
                        }
                    });
                }

                graphData.put("nodes", nodes);
                graphData.put("relationships", rels);
                return graphData;
            });
        }
    }
}
