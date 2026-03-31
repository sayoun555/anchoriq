package com.anchoriq.core.domain.maritime.news.gateway;

/**
 * 뉴스 데이터 외부 시스템 게이트웨이 인터페이스.
 * GNews API 연동 구현체가 이 인터페이스를 구현한다.
 */
public interface NewsDataGateway {

    /**
     * 해운 관련 최신 뉴스를 수집한다.
     */
    void collectLatestNews();
}
