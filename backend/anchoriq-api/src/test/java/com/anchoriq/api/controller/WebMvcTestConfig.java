package com.anchoriq.api.controller;

import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @WebMvcTest용 최소 Application 설정.
 * AnchoriqApplication의 @EnableJpaRepositories, @EnableNeo4jRepositories,
 * @EntityScan을 제외하여 슬라이스 테스트 시 JPA/Neo4j 빈 로드를 방지한다.
 *
 * Spring Boot의 @WebMvcTest는 가장 가까운 @SpringBootApplication을 찾아
 * 테스트 컨텍스트를 구성하므로, 이 클래스가 controller 패키지 내에 위치하면
 * AnchoriqApplication 대신 사용된다.
 */
@SpringBootApplication(scanBasePackages = "com.anchoriq.api")
class WebMvcTestConfig {
}
