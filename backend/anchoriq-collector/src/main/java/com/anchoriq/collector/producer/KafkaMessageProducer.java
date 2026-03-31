package com.anchoriq.collector.producer;

import java.util.Map;

/**
 * Kafka 메시지 Producer 공통 인터페이스.
 */
public interface KafkaMessageProducer {

    /**
     * 메시지를 Kafka 토픽으로 전송한다.
     *
     * @param key     파티션 키 (null 가능)
     * @param message 메시지 페이로드
     */
    void send(String key, Map<String, Object> message);

    /**
     * 대상 토픽 이름을 반환한다.
     */
    String topicName();
}
