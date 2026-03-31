package com.anchoriq.api.infrastructure.persistence.neo4j;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Values;
import org.neo4j.driver.types.Path;
import org.neo4j.driver.Session;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;

/**
 * 최단 경로 탐색 관련 Neo4j 쿼리 담당.
 */
@Component
@RequiredArgsConstructor
public class Neo4jPathFindingRepository {

    private final Driver driver;

    public List<Map<String, Object>> findShortestPath(Long fromNodeId, Long toNodeId) {
        String cypher = """
                MATCH (a) WHERE id(a) = $fromId
                MATCH (b) WHERE id(b) = $toId
                MATCH path = shortestPath((a)-[*..6]->(b))
                RETURN path
                """;

        try (Session session = driver.session()) {
            return session.executeRead(tx -> {
                var result = tx.run(cypher, Values.parameters("fromId", fromNodeId, "toId", toNodeId));
                List<Map<String, Object>> paths = new ArrayList<>();

                while (result.hasNext()) {
                    Record record = result.next();
                    Path path = record.get("path").asPath();
                    Map<String, Object> pathData = new HashMap<>();
                    List<Map<String, Object>> nodes = new ArrayList<>();
                    List<Map<String, Object>> rels = new ArrayList<>();

                    path.nodes().forEach(node -> nodes.add(Neo4jNodeHelper.toNodeMap(node)));
                    path.relationships().forEach(rel -> rels.add(Neo4jNodeHelper.toRelMap(rel)));

                    pathData.put("nodes", nodes);
                    pathData.put("relationships", rels);
                    pathData.put("length", path.length());
                    paths.add(pathData);
                }
                return paths;
            });
        }
    }
}
