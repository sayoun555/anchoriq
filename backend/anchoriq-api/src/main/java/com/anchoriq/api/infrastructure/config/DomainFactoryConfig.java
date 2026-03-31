package com.anchoriq.api.infrastructure.config;

import com.anchoriq.core.domain.account.subscription.factory.SubscriptionFactory;
import com.anchoriq.core.domain.account.subscription.repository.SubscriptionRepository;
import com.anchoriq.core.domain.maritime.sanction.repository.SanctionRepository;
import com.anchoriq.core.domain.maritime.vessel.factory.VesselFactory;
import com.anchoriq.core.domain.maritime.vessel.repository.VesselRepository;
import com.anchoriq.core.domain.operation.workflow.factory.WorkflowFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * DDD Factory Bean 설정.
 * 도메인 팩토리 클래스를 @Bean으로 명시적 등록한다.
 */
@Configuration
public class DomainFactoryConfig {

    @Bean
    public VesselFactory vesselFactory(VesselRepository vesselRepository,
                                        SanctionRepository sanctionRepository) {
        return new VesselFactory(vesselRepository, sanctionRepository);
    }

    @Bean
    public SubscriptionFactory subscriptionFactory(SubscriptionRepository subscriptionRepository) {
        return new SubscriptionFactory(subscriptionRepository);
    }

    @Bean
    public WorkflowFactory workflowFactory() {
        return new WorkflowFactory();
    }
}
