package com.anchoriq.api.infrastructure.persistence.neo4j;

import com.anchoriq.api.infrastructure.persistence.neo4j.node.Neo4jAnomalyDetectionNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;

import java.util.List;

/**
 * Spring Data Neo4j Repository: AnomalyDetection 노드 접근.
 */
public interface Neo4jAnomalyDetectionRepository extends Neo4jRepository<Neo4jAnomalyDetectionNode, String> {

    @Query("MATCH (a:AnomalyDetection) WHERE a.type = $type RETURN a")
    List<Neo4jAnomalyDetectionNode> findByType(String type);

    @Query("MATCH (a:AnomalyDetection) WHERE a.vesselImo = $vesselImo RETURN a")
    List<Neo4jAnomalyDetectionNode> findByVesselImo(String vesselImo);

    @Query("MATCH (a:AnomalyDetection) WHERE a.resolved = false RETURN a")
    List<Neo4jAnomalyDetectionNode> findUnresolved();

    @Query("MATCH (a:AnomalyDetection) WHERE a.type = $type RETURN a ORDER BY a.detectedAt DESC LIMIT $limit")
    List<Neo4jAnomalyDetectionNode> findRecentByType(String type, int limit);

    @Query("MATCH (a:AnomalyDetection) WHERE a.type = $type RETURN count(a)")
    long countByType(String type);
}
