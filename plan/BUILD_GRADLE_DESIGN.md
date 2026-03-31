# AnchorIQ — Gradle 멀티 모듈 의존성 설계

> 5개 모듈별 의존성 정리 — core는 순수 도메인, 인프라 의존성 금지

---

## 목차
- [모듈 구조](#모듈-구조)
- [루트 build.gradle](#루트-buildgradle)
- [anchoriq-core](#anchoriq-core)
- [anchoriq-api](#anchoriq-api)
- [anchoriq-collector](#anchoriq-collector)
- [anchoriq-ai](#anchoriq-ai)
- [anchoriq-automation](#anchoriq-automation)
- [모듈 간 의존성 다이어그램](#모듈-간-의존성-다이어그램)
- [settings.gradle](#settingsgradle)

---

## 모듈 구조

```
/backend/
  build.gradle              ← 루트 (공통 설정)
  settings.gradle           ← 모듈 등록
  /anchoriq-core/
    build.gradle            ← 순수 도메인 (인프라 의존성 X)
  /anchoriq-api/
    build.gradle            ← Spring Web + Security + Swagger
  /anchoriq-collector/
    build.gradle            ← Kafka + WebSocket + JSoup + WebClient
  /anchoriq-ai/
    build.gradle            ← OpenClaw + OpenPDF
  /anchoriq-automation/
    build.gradle            ← n8n + WebSocket
```

---

## 루트 build.gradle

> 공통 설정 — 모든 모듈에 적용

```groovy
plugins {
    id 'java'
    id 'org.springframework.boot' version '3.4.0'
    id 'io.spring.dependency-management' version '1.1.6'
}

allprojects {
    group = 'com.anchoriq'
    version = '0.0.1-SNAPSHOT'
}

subprojects {
    apply plugin: 'java'
    apply plugin: 'io.spring.dependency-management'

    java {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    repositories {
        mavenCentral()
    }

    dependencies {
        // 공통 — 모든 모듈에서 사용
        compileOnly 'org.projectlombok:lombok'
        annotationProcessor 'org.projectlombok:lombok'

        // 테스트 공통
        testImplementation 'org.springframework.boot:spring-boot-starter-test'
        testImplementation 'org.assertj:assertj-core'
    }

    test {
        useJUnitPlatform()
    }
}
```

---

## anchoriq-core

> 순수 도메인 — Entity, VO, Aggregate Root, Domain Service, Repository 인터페이스
> **Spring Web, Kafka, Neo4j 드라이버 등 인프라 의존성 금지**

```groovy
// anchoriq-core/build.gradle

dependencies {
    // JPA — Entity 어노테이션용 (@Entity, @Id, @Version 등)
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'

    // Neo4j — Node 어노테이션용 (@Node, @Relationship 등)
    implementation 'org.springframework.data:spring-data-neo4j'

    // Validation — 도메인 검증 (@NotNull, @Size 등)
    implementation 'org.springframework.boot:spring-boot-starter-validation'

    // MapStruct — DTO 매핑 (도메인 내 VO 변환용)
    implementation 'org.mapstruct:mapstruct:1.6.3'
    annotationProcessor 'org.mapstruct:mapstruct-processor:1.6.3'
}
```

### 이 모듈에 없는 것
```
❌ spring-boot-starter-web         → api 모듈에서
❌ spring-kafka                    → collector 모듈에서
❌ spring-data-redis               → api/collector 모듈에서
❌ spring-data-elasticsearch       → collector 모듈에서
❌ spring-security                 → api 모듈에서
```

---

## anchoriq-api

> REST Controller, DTO, 인증/인가, Swagger

```groovy
// anchoriq-api/build.gradle

apply plugin: 'org.springframework.boot'

dependencies {
    // 내부 모듈
    implementation project(':anchoriq-core')
    implementation project(':anchoriq-ai')

    // Spring Web
    implementation 'org.springframework.boot:spring-boot-starter-web'

    // Spring Security + JWT
    implementation 'org.springframework.boot:spring-boot-starter-security'
    implementation 'io.jsonwebtoken:jjwt-api:0.12.6'
    runtimeOnly 'io.jsonwebtoken:jjwt-impl:0.12.6'
    runtimeOnly 'io.jsonwebtoken:jjwt-jackson:0.12.6'

    // WebSocket (STOMP)
    implementation 'org.springframework.boot:spring-boot-starter-websocket'

    // Database
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-data-neo4j'
    implementation 'org.springframework.boot:spring-boot-starter-data-redis'
    implementation 'org.springframework.boot:spring-boot-starter-data-elasticsearch'
    runtimeOnly 'org.postgresql:postgresql'

    // Swagger
    implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui:2.7.0'

    // 내보내기
    implementation 'com.github.librepdf:openpdf:2.0.3'
    implementation 'com.opencsv:opencsv:5.9'

    // 환경변수
    implementation 'me.paulschwarz:spring-dotenv:4.0.0'

    // Actuator + Prometheus
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
    implementation 'io.micrometer:micrometer-registry-prometheus'

    // 결제
    implementation 'com.stripe:stripe-java:28.2.0'
    // Toss는 REST API 직접 호출 (별도 SDK 없음, WebClient 사용)

    // 테스트
    testImplementation 'org.springframework.security:spring-security-test'
    testImplementation 'org.testcontainers:junit-jupiter'
    testImplementation 'org.testcontainers:postgresql'
    testImplementation 'org.testcontainers:neo4j'
    testImplementation 'org.testcontainers:elasticsearch'
    testImplementation 'org.testcontainers:kafka'
}
```

### 왜 api에 DB 의존성이 다 있나?

```
anchoriq-api가 Spring Boot 메인 애플리케이션이기 때문.
다른 모듈은 라이브러리 모듈이고, api가 실행 가능한 JAR를 만든다.
모든 인프라 연결 설정(DataSource, Neo4jDriver 등)은 api 모듈에서 관리.
```

---

## anchoriq-collector

> 11개 API 수집기, UNCTAD 크롤러, Kafka Producer

```groovy
// anchoriq-collector/build.gradle

dependencies {
    // 내부 모듈
    implementation project(':anchoriq-core')

    // Kafka
    implementation 'org.springframework.kafka:spring-kafka'

    // WebSocket Client (AISstream)
    implementation 'org.springframework.boot:spring-boot-starter-websocket'

    // HTTP Client (11개 외부 API)
    implementation 'org.springframework.boot:spring-boot-starter-webflux'  // WebClient

    // 크롤링 (UNCTAD) — Playwright
    implementation 'com.microsoft.playwright:playwright:1.49.0'

    // XML 파싱 (UN 제재 목록)
    implementation 'jakarta.xml.bind:jakarta.xml.bind-api:4.0.2'
    runtimeOnly 'org.glassfish.jaxb:jaxb-runtime:4.0.5'

    // Redis (위치 데이터 GEO 저장)
    implementation 'org.springframework.boot:spring-boot-starter-data-redis'

    // Elasticsearch (뉴스 저장)
    implementation 'org.springframework.boot:spring-boot-starter-data-elasticsearch'

    // 테스트
    testImplementation 'org.springframework.kafka:spring-kafka-test'
    testImplementation 'org.testcontainers:kafka'
}
```

---

## anchoriq-ai

> OpenClaw 연동, 리스크 판단, 자연어 질의, 브리핑, What-if

```groovy
// anchoriq-ai/build.gradle

dependencies {
    // 내부 모듈
    implementation project(':anchoriq-core')

    // HTTP Client (OpenClaw API 호출)
    implementation 'org.springframework.boot:spring-boot-starter-webflux'  // WebClient

    // Neo4j (온톨로지 쿼리 — AI가 Cypher 생성)
    implementation 'org.springframework.boot:spring-boot-starter-data-neo4j'

    // Redis (AI 결과 캐싱, 리스크 스코어 캐싱)
    implementation 'org.springframework.boot:spring-boot-starter-data-redis'

    // Elasticsearch (AI 판단 로그 저장/검색)
    implementation 'org.springframework.boot:spring-boot-starter-data-elasticsearch'

    // PDF 리포트 생성
    implementation 'com.github.librepdf:openpdf:2.0.3'

    // JSON 처리 (OpenClaw 응답 파싱)
    implementation 'com.fasterxml.jackson.core:jackson-databind'
}
```

---

## anchoriq-automation

> n8n 연동, Kafka → 액션 처리, 알림

```groovy
// anchoriq-automation/build.gradle

dependencies {
    // 내부 모듈
    implementation project(':anchoriq-core')
    implementation project(':anchoriq-ai')

    // Kafka (리스크 알림 Consumer)
    implementation 'org.springframework.kafka:spring-kafka'

    // HTTP Client (n8n 웹훅 호출)
    implementation 'org.springframework.boot:spring-boot-starter-webflux'  // WebClient

    // WebSocket (프론트에 실시간 알림 푸시)
    implementation 'org.springframework.boot:spring-boot-starter-websocket'

    // Elasticsearch (알림 이력 저장)
    implementation 'org.springframework.boot:spring-boot-starter-data-elasticsearch'

    // 테스트
    testImplementation 'org.springframework.kafka:spring-kafka-test'
}
```

---

## 모듈 간 의존성 다이어그램

```
anchoriq-core (순수 도메인)
  ↑
  ├── anchoriq-api (실행 모듈, 모든 인프라 연결)
  │     ↑
  │     └── anchoriq-ai
  │
  ├── anchoriq-collector (데이터 수집)
  │
  ├── anchoriq-ai (AI 엔진)
  │
  └── anchoriq-automation (자동화)
        ↑
        └── anchoriq-ai
```

### 의존성 규칙

```
✅ anchoriq-api → anchoriq-core, anchoriq-ai
✅ anchoriq-collector → anchoriq-core
✅ anchoriq-ai → anchoriq-core
✅ anchoriq-automation → anchoriq-core, anchoriq-ai

❌ anchoriq-core → 다른 모듈 (순수 도메인, 외부 의존 금지)
❌ anchoriq-collector → anchoriq-api (수집기가 API에 의존하면 안 됨)
❌ 순환 의존 (A → B → A 금지)
```

---

## settings.gradle

```groovy
rootProject.name = 'anchoriq'

include 'anchoriq-core'
include 'anchoriq-api'
include 'anchoriq-collector'
include 'anchoriq-ai'
include 'anchoriq-automation'
```

---

## 버전 관리

> 외부 라이브러리 버전은 루트 `build.gradle`에서 변수로 관리

```groovy
// 루트 build.gradle
ext {
    jjwtVersion = '0.12.6'
    mapstructVersion = '1.6.3'
    playwrightVersion = '1.49.0'
    openpdfVersion = '2.0.3'
    opencsvVersion = '5.9'
    springdocVersion = '2.7.0'
    stripeVersion = '28.2.0'
    testcontainersVersion = '1.20.4'
}
```

> Spring 관련 버전은 `spring-boot-dependencies` BOM이 자동 관리 → 명시 불필요
