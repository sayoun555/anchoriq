package com.anchoriq.api.infrastructure.config;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Micrometer 메트릭 공통 설정.
 * 모든 메트릭에 application=anchoriq 태그를 추가하여
 * Prometheus/Grafana에서 필터링할 수 있도록 한다.
 */
@Configuration
public class MetricsConfig {

    @Bean
    public MeterRegistryCustomizer<MeterRegistry> commonTags() {
        return registry -> registry.config()
                .commonTags("application", "anchoriq");
    }
}
