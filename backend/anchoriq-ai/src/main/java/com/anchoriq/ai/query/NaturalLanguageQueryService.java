package com.anchoriq.ai.query;

import java.util.List;
import java.util.Map;

/**
 * 자연어 질의 서비스 인터페이스.
 * 사용자의 자연어 질문을 Cypher 쿼리로 변환하고 실행한다.
 */
public interface NaturalLanguageQueryService {

    /**
     * 자연어 질문을 Neo4j Cypher로 변환하여 실행하고, AI 자연어 응답을 생성한다.
     *
     * @param query 사용자의 자연어 질문
     * @return answer (자연어 응답), entities (관련 엔티티), cypher (생성된 쿼리)
     */
    Map<String, Object> executeQuery(String query);
}
