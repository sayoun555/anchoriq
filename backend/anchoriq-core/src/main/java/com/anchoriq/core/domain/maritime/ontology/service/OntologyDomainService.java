package com.anchoriq.core.domain.maritime.ontology.service;

import java.util.List;
import java.util.Map;

/**
 * 온톨로지 Domain Service 인터페이스.
 * 여러 Aggregate에 걸치는 그래프 탐색 로직을 담당한다.
 */
public interface OntologyDomainService {

    /**
     * 노드 클릭 시 연결된 노드/관계를 반환한다.
     */
    Map<String, Object> expandNode(Long nodeId, int depth);

    /**
     * 두 엔티티 간 최단 경로를 찾는다.
     */
    List<Map<String, Object>> findShortestPath(Long fromNodeId, Long toNodeId);

    /**
     * 제재 연결 그래프를 반환한다.
     */
    Map<String, Object> findSanctionNetwork();

    /**
     * 온톨로지 통계 (노드 수, 관계 수)를 반환한다.
     */
    Map<String, Long> getStatistics();

    /**
     * 엔티티를 검색한다.
     */
    List<Map<String, Object>> searchEntities(String query, int limit);

    /**
     * 그래프 뷰 초기 데이터를 반환한다.
     */
    Map<String, Object> getGraphOverview(int nodeLimit);
}
