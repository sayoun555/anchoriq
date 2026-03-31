package com.anchoriq.ai.consumer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * AI 판단 결과를 Elasticsearch에 저장하는 Consumer.
 * Tier 3 (로그 저장) - 실패해도 서비스에 영향 없음.
 * Elasticsearch가 비활성화된 환경에서는 Bean이 생성되지 않는다.
 */
@Slf4j
@Component
@ConditionalOnBean(ElasticsearchOperations.class)
public class AiDecisionLogConsumer {

    private static final IndexCoordinates AI_DECISIONS_INDEX = IndexCoordinates.of("ai-decisions");

    private final ElasticsearchOperations esOperations;

    public AiDecisionLogConsumer(ElasticsearchOperations esOperations) {
        this.esOperations = esOperations;
    }

    @Async
    public void logDecision(String queryType, String query, String result,
                            Long userId, int tokensUsed) {
        try {
            Map<String, Object> document = new HashMap<>();
            document.put("queryType", queryType);
            document.put("query", query);
            document.put("result", result);
            document.put("userId", userId);
            document.put("tokensUsed", tokensUsed);
            document.put("timestamp", Instant.now().toString());

            esOperations.save(document, AI_DECISIONS_INDEX);
            log.debug("AI decision logged: type={}, userId={}", queryType, userId);
        } catch (Exception e) {
            log.warn("Failed to log AI decision to Elasticsearch, ignoring: {}", e.getMessage());
        }
    }
}
