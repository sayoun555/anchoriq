package com.anchoriq.api.infrastructure.persistence.neo4j;

import com.anchoriq.api.infrastructure.persistence.neo4j.node.Neo4jWeatherConditionNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;

import java.util.List;

public interface Neo4jWeatherRepository extends Neo4jRepository<Neo4jWeatherConditionNode, Long> {

    @Query("MATCH (w:WeatherCondition) WHERE w.severity IN ['HIGH', 'CRITICAL'] RETURN w")
    List<Neo4jWeatherConditionNode> findSevereConditions();

    @Query("MATCH (w:WeatherCondition) WHERE w.type = 'TYPHOON' RETURN w")
    List<Neo4jWeatherConditionNode> findActiveTyphoons();
}
