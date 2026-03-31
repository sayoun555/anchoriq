package com.anchoriq.core.domain.maritime.sanction.gateway;

/**
 * 제재 데이터 외부 시스템 게이트웨이 인터페이스.
 * UN 제재 목록 / OpenSanctions 연동 구현체가 이 인터페이스를 구현한다.
 */
public interface SanctionDataGateway {

    /**
     * 최신 제재 목록을 수집한다.
     */
    void collectSanctionUpdates();
}
