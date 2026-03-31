## Neo4j TransactionTemplate null → Neo4jTransactionManager 명시 등록으로 해결

### 배경 및 문제정의
- **상황**: Spring Boot 앱 시작 시 GeographyInitializer가 Neo4j에 초크포인트/항만 데이터를 시딩하려고 시도
- **문제**: 모든 Neo4j Repository 호출에서 `Cannot invoke "TransactionTemplate.execute()" because "this.this$0.transactionTemplate" is null` 에러 발생. 데이터가 전혀 들어가지 않음

### 기술 선정 (대안 비교 테이블)

| 항목 | Neo4jTransactionManager 명시 등록 | @Primary로 JPA TM 대체 | TransactionTemplate 수동 생성 |
|------|----------------------------------|----------------------|----------------------------|
| 정석 여부 | Spring Data Neo4j 공식 권장 | 위험 (JPA 트랜잭션 깨짐) | 우회 해법 |
| JPA 영향 | 없음 (별도 이름으로 등록) | JPA 기본 TM 덮어씀 | 없음 |
| 유지보수 | 간단 | 복잡 | 복잡 |

### 분석
- **초기 판단**: Repository 구현체 코드 문제로 판단 → 코드에 `TransactionTemplate` 문자열 없음
- **실제 원인**: Spring Data Neo4j의 `SimpleNeo4jRepository` 내부에서 `Neo4jTransactionManager`가 주입되어야 `TransactionTemplate`이 생성됨. JPA와 Neo4j 2개의 데이터 소스가 공존할 때, Spring Boot가 JPA의 `PlatformTransactionManager`를 기본으로 사용하고 Neo4j용 트랜잭션 매니저를 자동 생성하지 못함
- **CS 원리**: Spring의 `@EnableNeo4jRepositories`는 내부적으로 `Neo4jTransactionManager` Bean을 찾아서 Repository proxy에 주입한다. 멀티 데이터소스 환경에서는 `transactionManagerRef`로 명시해야 올바른 TM이 매핑됨

### 솔루션

```java
// Neo4jConfig.java
@Configuration
public class Neo4jConfig {
    @Bean("neo4jTransactionManager")
    public PlatformTransactionManager neo4jTransactionManager(Driver driver) {
        return new Neo4jTransactionManager(driver);
    }
}

// AnchoriqApplication.java
@EnableNeo4jRepositories(
    basePackages = "com.anchoriq.api.infrastructure.persistence.neo4j",
    transactionManagerRef = "neo4jTransactionManager"  // 명시적 참조
)
```

### 결과 (Before/After 수치 테이블)

| 지표 | Before | After |
|------|--------|-------|
| 초크포인트 시딩 | 0개 (에러) | 6개 성공 |
| EEZ 시딩 | 0개 (에러) | 13개 성공 |
| 항만 시딩 | 0개 (에러) | 20개 성공 |
| 데모 데이터 시딩 | 0개 (에러) | 11국가 + 6회사 + 25선박 성공 |
| Neo4j Repository 전체 | 모든 쿼리 실패 | 정상 동작 |
