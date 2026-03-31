package com.anchoriq.ai.query;

import com.anchoriq.ai.client.AiClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 자연어 질의 서비스 구현체.
 * LLM에게 Neo4j 스키마 + 사용자 질문을 전달하여 Cypher를 생성하고 실행한다.
 */
@Slf4j
@Service
public class NaturalLanguageQueryServiceImpl implements NaturalLanguageQueryService {

    private static final String NEO4J_SCHEMA_PROMPT = """
            You are a Neo4j Cypher query expert for a maritime supply chain risk platform.

            Neo4j Schema:
            Nodes: Vessel (imo, mmsi, name, flag, type, status), Port (locode, name, country, latitude, longitude, congestionLevel),
            Route (name, displayName, distance, unit), Chokepoint (name, displayName, lat, lon, riskLevel, transitVolume),
            Company (name, country), Country (code, name), Sanction (referenceNumber, targetName, type, source, active),
            WeatherCondition (type, severity, lat, lon), SeaZone (name, lat, lon), Eez (name, country)

            Relationships: OWNED_BY (Vessel->Company), REGISTERED_IN (Company->Country),
            SANCTIONED_BY (Country->Sanction), PASSES_THROUGH (Route->Chokepoint),
            SAILING_ON (Vessel->Route), DOCKED_AT (Vessel->Port), HAS_WEATHER (SeaZone->WeatherCondition),
            JURISDICTION (SeaZone->Eez)

            Rules:
            1. Generate ONLY read-only Cypher (MATCH/RETURN only, NO CREATE/DELETE/SET/MERGE)
            2. Always specify relationship direction
            3. Always specify node labels in MATCH patterns
            4. Return only the Cypher query, no explanation
            """;

    private static final String RESPONSE_GENERATION_PROMPT = """
            You are a maritime intelligence analyst. Given the query results from a Neo4j database,
            provide a clear, concise natural language summary. Focus on actionable insights.
            Keep the response professional and under 200 words.
            """;

    private final AiClient aiClient;
    private final Neo4jClient neo4jClient;

    public NaturalLanguageQueryServiceImpl(AiClient aiClient, Neo4jClient neo4jClient) {
        this.aiClient = aiClient;
        this.neo4jClient = neo4jClient;
    }

    @Override
    public Map<String, Object> executeQuery(String query) {
        Map<String, Object> result = new HashMap<>();

        String cypher = generateCypher(query);
        result.put("cypher", cypher);

        CypherQueryValidator.validateReadOnly(cypher);

        List<Map<String, Object>> queryResults = executeCypher(cypher);
        result.put("entities", queryResults);

        String answer = generateNaturalLanguageResponse(query, queryResults);
        result.put("answer", answer);

        return result;
    }

    private String generateCypher(String query) {
        String cypher = aiClient.chat(NEO4J_SCHEMA_PROMPT, query, 0.1)
                .block();

        if (cypher == null || cypher.isBlank()) {
            throw new IllegalStateException("Failed to generate Cypher query from natural language");
        }

        return cypher.trim()
                .replace("```cypher", "")
                .replace("```", "")
                .trim();
    }

    private List<Map<String, Object>> executeCypher(String cypher) {
        try {
            Collection<Map<String, Object>> records = neo4jClient.query(cypher)
                    .fetch()
                    .all();
            return new ArrayList<>(records);
        } catch (Exception e) {
            log.error("Failed to execute Cypher query: {} - Error: {}", cypher, e.getMessage());
            return List.of();
        }
    }

    private String generateNaturalLanguageResponse(String originalQuery,
                                                     List<Map<String, Object>> results) {
        String userMessage = String.format(
                "User question: %s\n\nQuery results (%d records): %s",
                originalQuery, results.size(), formatResults(results));

        String response = aiClient.chat(RESPONSE_GENERATION_PROMPT, userMessage)
                .block();

        return response != null ? response : "Unable to generate response at this time.";
    }

    private String formatResults(List<Map<String, Object>> results) {
        if (results.isEmpty()) {
            return "No results found.";
        }
        int limit = Math.min(results.size(), 10);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < limit; i++) {
            sb.append(results.get(i).toString());
            if (i < limit - 1) {
                sb.append("\n");
            }
        }
        if (results.size() > 10) {
            sb.append("\n... and ").append(results.size() - 10).append(" more records");
        }
        return sb.toString();
    }
}
