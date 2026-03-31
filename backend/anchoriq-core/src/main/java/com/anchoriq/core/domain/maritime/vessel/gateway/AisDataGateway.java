package com.anchoriq.core.domain.maritime.vessel.gateway;

/**
 * AIS 데이터 외부 시스템 게이트웨이 인터페이스.
 * 구현체는 collector 모듈의 infrastructure에 위치한다.
 */
public interface AisDataGateway {

    /**
     * AIS WebSocket 스트림을 시작한다.
     */
    void startStreaming();

    /**
     * AIS WebSocket 스트림을 중지한다.
     */
    void stopStreaming();

    /**
     * 현재 스트리밍 중인지 확인한다.
     */
    boolean isStreaming();
}
