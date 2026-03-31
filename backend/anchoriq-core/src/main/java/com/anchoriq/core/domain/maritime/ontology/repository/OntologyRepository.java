package com.anchoriq.core.domain.maritime.ontology.repository;

import java.util.List;
import java.util.Map;

/**
 * 온톨로지 그래프 탐색 Repository 인터페이스.
 * 커스텀 Cypher 쿼리 기반으로 노드/관계 탐색을 수행한다.
 */
public interface OntologyRepository {

    /**
     * 노드 클릭 시 연결된 노드와 관계를 반환한다.
     * 4홉 쿼리 최적화: 중간 노드 라벨 항상 명시, 양방향 탐색 금지.
     */
    Map<String, Object> expandNode(Long nodeId, int depth);

    /**
     * 두 엔티티 간 최단 경로를 찾는다.
     */
    List<Map<String, Object>> findShortestPath(Long fromNodeId, Long toNodeId);

    /**
     * 제재 연관 네트워크 그래프를 반환한다.
     * Sanction → Country/Company/Vessel 관계를 탐색한다.
     */
    Map<String, Object> findSanctionNetwork();

    /**
     * 온톨로지 통계 (노드 수, 관계 수)를 반환한다.
     */
    Map<String, Long> getStatistics();

    /**
     * 엔티티를 검색한다 (Vessel, Port, Company, Country).
     */
    List<Map<String, Object>> searchEntities(String query, int limit);

    /**
     * 회사 소유 선박 목록을 반환한다.
     */
    List<Map<String, Object>> findVesselsByCompany(String companyName);

    /**
     * 국적별 선박 목록을 반환한다.
     */
    List<Map<String, Object>> findVesselsByCountry(String countryCode);

    /**
     * 그래프 뷰 초기 데이터를 반환한다.
     */
    Map<String, Object> getGraphOverview(int nodeLimit);
}
