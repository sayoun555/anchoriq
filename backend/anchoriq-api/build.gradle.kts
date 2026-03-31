plugins {
    java
    id("org.springframework.boot")
}

tasks.named<org.springframework.boot.gradle.tasks.run.BootRun>("bootRun") {
    workingDir = rootProject.projectDir
}

dependencies {
    // Internal modules
    implementation(project(":anchoriq-core"))
    implementation(project(":anchoriq-ai"))
    implementation(project(":anchoriq-automation"))
    implementation(project(":anchoriq-collector"))

    // Spring Web
    implementation("org.springframework.boot:spring-boot-starter-web")

    // WebClient (for external API calls - non-blocking HTTP client)
    implementation("org.springframework.boot:spring-boot-starter-webflux")

    // AOP (for @RequiresPlan aspect)
    implementation("org.springframework.boot:spring-boot-starter-aop")

    // Validation
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // Spring Security + JWT
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("io.jsonwebtoken:jjwt-api:${rootProject.extra["jjwtVersion"]}")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:${rootProject.extra["jjwtVersion"]}")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:${rootProject.extra["jjwtVersion"]}")

    // WebSocket (STOMP)
    implementation("org.springframework.boot:spring-boot-starter-websocket")

    // Kafka
    implementation("org.springframework.kafka:spring-kafka")

    // Database
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-data-neo4j")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-data-elasticsearch")
    runtimeOnly("org.postgresql:postgresql")

    // Swagger
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:${rootProject.extra["springdocVersion"]}")

    // Export
    implementation("com.github.librepdf:openpdf:${rootProject.extra["openpdfVersion"]}")
    implementation("com.opencsv:opencsv:${rootProject.extra["opencsvVersion"]}")

    // Environment variables
    implementation("me.paulschwarz:spring-dotenv:4.0.0")

    // Actuator + Prometheus
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("io.micrometer:micrometer-registry-prometheus")

    // Payment
    implementation("com.stripe:stripe-java:${rootProject.extra["stripeVersion"]}")

    // Test
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.testcontainers:junit-jupiter:${rootProject.extra["testcontainersVersion"]}")
    testImplementation("org.testcontainers:postgresql:${rootProject.extra["testcontainersVersion"]}")
    testImplementation("org.testcontainers:neo4j:${rootProject.extra["testcontainersVersion"]}")
    testImplementation("org.testcontainers:elasticsearch:${rootProject.extra["testcontainersVersion"]}")
    testImplementation("org.testcontainers:kafka:${rootProject.extra["testcontainersVersion"]}")
}
