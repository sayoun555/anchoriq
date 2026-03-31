plugins {
    java
}

dependencies {
    // Internal modules
    implementation(project(":anchoriq-core"))

    // Kafka
    implementation("org.springframework.kafka:spring-kafka")

    // WebSocket Client (AISstream)
    implementation("org.springframework.boot:spring-boot-starter-websocket")

    // HTTP Client (11 external APIs)
    implementation("org.springframework.boot:spring-boot-starter-webflux")

    // Crawling (UNCTAD) - Playwright
    implementation("com.microsoft.playwright:playwright:${rootProject.extra["playwrightVersion"]}")

    // XML parsing (UN sanctions)
    implementation("jakarta.xml.bind:jakarta.xml.bind-api:4.0.2")
    runtimeOnly("org.glassfish.jaxb:jaxb-runtime:4.0.5")

    // Redis (vessel position GEO storage)
    implementation("org.springframework.boot:spring-boot-starter-data-redis")

    // Elasticsearch (news storage)
    implementation("org.springframework.boot:spring-boot-starter-data-elasticsearch")

    // Test
    testImplementation("org.springframework.kafka:spring-kafka-test")
    testImplementation("org.testcontainers:kafka:${rootProject.extra["testcontainersVersion"]}")
}

// Library module
tasks.named<Jar>("jar") {
    enabled = true
}
