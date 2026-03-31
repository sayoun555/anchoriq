package com.anchoriq.collector.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.TopicConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;

import java.time.Duration;

/**
 * Kafka 토픽 선언적 생성 설정.
 * 8개 토픽을 Bean으로 선언하여 애플리케이션 시작 시 자동 생성한다.
 * KafkaAutoConfiguration이 비활성화된 환경에서는 토픽을 생성하지 않는다.
 */
@Configuration
@ConditionalOnBean(KafkaAdmin.class)
public class KafkaTopicConfig {

    public static final String AIS_POSITIONS = "ais-positions";
    public static final String WEATHER_EVENTS = "weather-events";
    public static final String SANCTION_UPDATES = "sanction-updates";
    public static final String NEWS_EVENTS = "news-events";
    public static final String MARKET_DATA = "market-data";
    public static final String GEOPOLITICAL_EVENTS = "geopolitical-events";
    public static final String PORT_CONGESTION = "port-congestion";
    public static final String RISK_ALERTS = "risk-alerts";

    @Bean
    public NewTopic aisPositions() {
        return TopicBuilder.name(AIS_POSITIONS)
                .partitions(3)
                .replicas(1)
                .config(TopicConfig.RETENTION_MS_CONFIG,
                        String.valueOf(Duration.ofHours(1).toMillis()))
                .build();
    }

    @Bean
    public NewTopic weatherEvents() {
        return TopicBuilder.name(WEATHER_EVENTS)
                .partitions(1)
                .replicas(1)
                .config(TopicConfig.RETENTION_MS_CONFIG,
                        String.valueOf(Duration.ofHours(24).toMillis()))
                .build();
    }

    @Bean
    public NewTopic sanctionUpdates() {
        return TopicBuilder.name(SANCTION_UPDATES)
                .partitions(1)
                .replicas(1)
                .config(TopicConfig.RETENTION_MS_CONFIG,
                        String.valueOf(Duration.ofDays(7).toMillis()))
                .build();
    }

    @Bean
    public NewTopic newsEvents() {
        return TopicBuilder.name(NEWS_EVENTS)
                .partitions(1)
                .replicas(1)
                .config(TopicConfig.RETENTION_MS_CONFIG,
                        String.valueOf(Duration.ofDays(3).toMillis()))
                .build();
    }

    @Bean
    public NewTopic marketData() {
        return TopicBuilder.name(MARKET_DATA)
                .partitions(1)
                .replicas(1)
                .config(TopicConfig.RETENTION_MS_CONFIG,
                        String.valueOf(Duration.ofDays(3).toMillis()))
                .build();
    }

    @Bean
    public NewTopic geopoliticalEvents() {
        return TopicBuilder.name(GEOPOLITICAL_EVENTS)
                .partitions(1)
                .replicas(1)
                .config(TopicConfig.RETENTION_MS_CONFIG,
                        String.valueOf(Duration.ofDays(3).toMillis()))
                .build();
    }

    @Bean
    public NewTopic portCongestion() {
        return TopicBuilder.name(PORT_CONGESTION)
                .partitions(1)
                .replicas(1)
                .config(TopicConfig.RETENTION_MS_CONFIG,
                        String.valueOf(Duration.ofHours(24).toMillis()))
                .build();
    }

    @Bean
    public NewTopic riskAlerts() {
        return TopicBuilder.name(RISK_ALERTS)
                .partitions(2)
                .replicas(1)
                .config(TopicConfig.RETENTION_MS_CONFIG,
                        String.valueOf(Duration.ofDays(7).toMillis()))
                .build();
    }
}
