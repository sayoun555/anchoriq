package com.anchoriq.collector.source;

/**
 * 데이터 수집기 공통 인터페이스.
 * 모든 REST API 수집기가 이 인터페이스를 구현한다.
 */
public interface DataCollector {

    /**
     * 데이터를 수집하여 Kafka 토픽으로 전송한다.
     */
    void collect();

    /**
     * 수집기 이름을 반환한다.
     */
    String sourceName();
}
