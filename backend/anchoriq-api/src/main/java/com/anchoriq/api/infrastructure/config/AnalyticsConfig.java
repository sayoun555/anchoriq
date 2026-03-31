package com.anchoriq.api.infrastructure.config;

import com.anchoriq.api.infrastructure.analytics.AiAnalyticsServiceImpl;
import com.anchoriq.api.infrastructure.analytics.CollectorStatisticsServiceImpl;
import com.anchoriq.api.infrastructure.analytics.GeopoliticalAnalyticsServiceImpl;
import com.anchoriq.api.infrastructure.analytics.MarketAnalyticsServiceImpl;
import com.anchoriq.api.infrastructure.analytics.PortAnalyticsServiceImpl;
import com.anchoriq.api.infrastructure.analytics.RouteAnalyticsServiceImpl;
import com.anchoriq.api.infrastructure.analytics.VesselAnalyticsServiceImpl;
import com.anchoriq.core.domain.analytics.service.AiAnalyticsService;
import com.anchoriq.core.domain.analytics.service.CollectorStatisticsService;
import com.anchoriq.core.domain.analytics.service.GeopoliticalAnalyticsService;
import com.anchoriq.core.domain.analytics.service.MarketAnalyticsService;
import com.anchoriq.core.domain.analytics.service.PortAnalyticsService;
import com.anchoriq.core.domain.analytics.service.RouteAnalyticsService;
import com.anchoriq.core.domain.analytics.service.VesselAnalyticsService;
import com.anchoriq.core.domain.maritime.port.repository.PortRepository;
import com.anchoriq.core.domain.maritime.route.repository.ChokepointRepository;
import com.anchoriq.core.domain.maritime.route.repository.RouteRepository;
import com.anchoriq.core.domain.maritime.vessel.repository.VesselRepository;
import com.anchoriq.core.domain.operation.collector.service.CollectorRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 통계/분석 서비스 Bean 설정.
 * 7개 Analytics Service 구현체를 @Configuration + @Bean으로 명시적 등록한다.
 * 구현체에서 @Service를 제거하고 순수 POJO로 유지한다.
 */
@Configuration
public class AnalyticsConfig {

    @Bean
    @ConditionalOnBean(CollectorRegistry.class)
    public CollectorStatisticsService collectorStatisticsService(
            StringRedisTemplate redisTemplate,
            CollectorRegistry collectorRegistry) {
        return new CollectorStatisticsServiceImpl(redisTemplate, collectorRegistry);
    }

    @Bean
    public VesselAnalyticsService vesselAnalyticsService(
            VesselRepository vesselRepository) {
        return new VesselAnalyticsServiceImpl(vesselRepository);
    }

    @Bean
    public PortAnalyticsService portAnalyticsService(
            PortRepository portRepository,
            StringRedisTemplate redisTemplate) {
        return new PortAnalyticsServiceImpl(portRepository, redisTemplate);
    }

    @Bean
    public RouteAnalyticsService routeAnalyticsService(
            RouteRepository routeRepository,
            ChokepointRepository chokepointRepository,
            StringRedisTemplate redisTemplate) {
        return new RouteAnalyticsServiceImpl(routeRepository, chokepointRepository, redisTemplate);
    }

    @Bean
    public AiAnalyticsService aiAnalyticsService(
            StringRedisTemplate redisTemplate) {
        return new AiAnalyticsServiceImpl(redisTemplate);
    }

    @Bean
    public MarketAnalyticsService marketAnalyticsService(
            StringRedisTemplate redisTemplate) {
        return new MarketAnalyticsServiceImpl(redisTemplate);
    }

    @Bean
    public GeopoliticalAnalyticsService geopoliticalAnalyticsService(
            StringRedisTemplate redisTemplate) {
        return new GeopoliticalAnalyticsServiceImpl(redisTemplate);
    }
}
