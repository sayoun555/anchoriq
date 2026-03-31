package com.anchoriq.api.infrastructure.persistence.neo4j;

import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Relationship;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Neo4j Node/Relationship를 Map으로 변환하는 유틸리티.
 */
public final class Neo4jNodeHelper {

    private Neo4jNodeHelper() {
    }

    public static Map<String, Object> toNodeMap(Node node) {
        Map<String, Object> map = new HashMap<>(node.asMap());
        map.put("_id", node.id());
        map.put("_labels", StreamSupport.stream(node.labels().spliterator(), false)
                .collect(Collectors.toList()));
        return map;
    }

    public static Map<String, Object> toRelMap(Relationship rel) {
        Map<String, Object> map = new HashMap<>(rel.asMap());
        map.put("_id", rel.id());
        map.put("_type", rel.type());
        map.put("_startNodeId", rel.startNodeId());
        map.put("_endNodeId", rel.endNodeId());
        return map;
    }
}
