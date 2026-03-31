package com.anchoriq.api.infrastructure.config;

import com.anchoriq.api.infrastructure.subscription.SubscriptionServiceImpl;
import com.anchoriq.core.domain.account.subscription.repository.SubscriptionRepository;
import com.anchoriq.core.domain.account.subscription.service.SubscriptionService;
import com.anchoriq.core.domain.intelligence.anomaly.repository.AnomalyRepository;
import com.anchoriq.core.domain.intelligence.anomaly.service.AnomalyDetectionService;
import com.anchoriq.core.domain.intelligence.anomaly.service.AnomalyDetectionServiceImpl;
import com.anchoriq.core.domain.intelligence.risk.service.RouteComparisonService;
import com.anchoriq.core.domain.intelligence.risk.service.RouteComparisonServiceImpl;
import com.anchoriq.core.domain.intelligence.risk.service.RouteOptimizationService;
import com.anchoriq.core.domain.intelligence.risk.service.RouteOptimizationServiceImpl;
import com.anchoriq.core.domain.intelligence.risk.service.SanctionScreeningService;
import com.anchoriq.core.domain.intelligence.risk.service.SanctionScreeningServiceImpl;
import com.anchoriq.core.domain.intelligence.risk.service.SupplyChainRiskService;
import com.anchoriq.core.domain.intelligence.risk.service.SupplyChainRiskServiceImpl;
import com.anchoriq.core.domain.maritime.port.repository.PortRepository;
import com.anchoriq.core.domain.maritime.route.repository.RouteRepository;
import com.anchoriq.core.domain.maritime.sanction.repository.SanctionRepository;
import com.anchoriq.core.domain.maritime.vessel.repository.VesselRepository;
import com.anchoriq.core.domain.maritime.weather.repository.WeatherRepository;
import com.anchoriq.core.domain.operation.notification.service.NotificationDomainService;
import com.anchoriq.core.domain.operation.notification.service.NotificationDomainServiceImpl;
import com.anchoriq.core.domain.operation.workflow.service.WorkflowDomainService;
import com.anchoriq.core.domain.operation.workflow.service.WorkflowDomainServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 도메인 서비스 Bean 설정.
 * core 모듈의 Domain Service 구현체를 @Configuration + @Bean으로 명시적 등록한다.
 * 구현체에서 @Service를 제거하고 순수 POJO로 유지한다.
 */
@Configuration
public class DomainServiceConfig {

    @Bean
    public SupplyChainRiskService supplyChainRiskService(
            SanctionRepository sanctionRepository,
            WeatherRepository weatherRepository) {
        return new SupplyChainRiskServiceImpl(sanctionRepository, weatherRepository);
    }

    @Bean
    public SanctionScreeningService sanctionScreeningService(
            SanctionRepository sanctionRepository,
            VesselRepository vesselRepository) {
        return new SanctionScreeningServiceImpl(sanctionRepository, vesselRepository);
    }

    @Bean
    public RouteOptimizationService routeOptimizationService(
            RouteRepository routeRepository) {
        return new RouteOptimizationServiceImpl(routeRepository);
    }

    @Bean
    public AnomalyDetectionService anomalyDetectionService(
            AnomalyRepository anomalyRepository) {
        return new AnomalyDetectionServiceImpl(anomalyRepository);
    }

    @Bean
    public RouteComparisonService routeComparisonService(
            RouteRepository routeRepository,
            PortRepository portRepository,
            SupplyChainRiskService supplyChainRiskService) {
        return new RouteComparisonServiceImpl(routeRepository, portRepository, supplyChainRiskService);
    }

    @Bean
    public WorkflowDomainService workflowDomainService() {
        return new WorkflowDomainServiceImpl();
    }

    @Bean
    public NotificationDomainService notificationDomainService() {
        return new NotificationDomainServiceImpl();
    }

    @Bean
    public SubscriptionService subscriptionService(
            SubscriptionRepository subscriptionRepository,
            StringRedisTemplate stringRedisTemplate) {
        return new SubscriptionServiceImpl(subscriptionRepository, stringRedisTemplate);
    }
}
