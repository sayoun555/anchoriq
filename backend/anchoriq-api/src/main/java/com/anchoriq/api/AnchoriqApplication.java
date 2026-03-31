package com.anchoriq.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;

@SpringBootApplication(scanBasePackages = "com.anchoriq")
@EntityScan(basePackages = "com.anchoriq.core.domain")
@EnableJpaRepositories(basePackages = {
        "com.anchoriq.api.infrastructure.persistence"
})
@EnableNeo4jRepositories(
        basePackages = "com.anchoriq.api.infrastructure.persistence.neo4j",
        transactionManagerRef = "neo4jTransactionManager"
)
public class AnchoriqApplication {

    public static void main(String[] args) {
        SpringApplication.run(AnchoriqApplication.class, args);
    }
}
