package com.anchoriq.automation.consumer;

/**
 * Automation 모듈은 별도 Kafka Producer를 생성하지 않는다.
 * anchoriq-collector의 KafkaProducerConfig에서 생성한 KafkaTemplate을 공유한다.
 * 모듈 간 Bean 중복을 방지하기 위한 정석적 설계.
 */
// 빈 클래스 — 향후 automation 전용 Producer 설정이 필요하면 여기에 추가
