package com.anchoriq.api.infrastructure.config;

import com.anchoriq.core.domain.maritime.ontology.repository.OntologyRepository;
import com.anchoriq.core.domain.maritime.ontology.service.OntologyDomainService;
import com.anchoriq.core.domain.maritime.ontology.service.OntologyDomainServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OntologyConfig {

    @Bean
    public OntologyDomainService ontologyDomainService(OntologyRepository ontologyRepository) {
        return new OntologyDomainServiceImpl(ontologyRepository);
    }
}
