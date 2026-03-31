package com.anchoriq.api.infrastructure.config;

import jakarta.persistence.EntityManagerFactory;
import org.neo4j.driver.Driver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.neo4j.core.transaction.Neo4jTransactionManager;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * 멀티 데이터소스 트랜잭션 매니저 설정.
 * JPA(PostgreSQL)와 Neo4j가 함께 사용될 때 각각의 트랜잭션 매니저를 명시적으로 등록한다.
 * JPA를 @Primary로 설정하여 @Transactional 기본 동작을 보장한다.
 */
@Configuration
public class Neo4jConfig {

    @Primary
    @Bean("transactionManager")
    public PlatformTransactionManager jpaTransactionManager(EntityManagerFactory emf) {
        return new JpaTransactionManager(emf);
    }

    @Bean("neo4jTransactionManager")
    public PlatformTransactionManager neo4jTransactionManager(Driver driver) {
        return new Neo4jTransactionManager(driver);
    }
}
