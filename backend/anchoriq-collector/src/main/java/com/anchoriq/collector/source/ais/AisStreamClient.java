package com.anchoriq.collector.source.ais;

/**
 * AIS 스트림 클라이언트 인터페이스.
 * WebSocket 기반 AIS 데이터 수신.
 */
public interface AisStreamClient {

    /**
     * AIS WebSocket 연결을 시작한다.
     */
    void connect();

    /**
     * AIS WebSocket 연결을 종료한다.
     */
    void disconnect();

    /**
     * 현재 연결 상태를 확인한다.
     */
    boolean isConnected();
}
