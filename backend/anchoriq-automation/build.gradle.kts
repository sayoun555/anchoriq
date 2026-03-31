plugins {
    java
}

dependencies {
    // Internal modules
    implementation(project(":anchoriq-core"))
    implementation(project(":anchoriq-ai"))

    // Spring Data (for repository interfaces from core)
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")

    // Kafka (risk alert consumer)
    implementation("org.springframework.kafka:spring-kafka")

    // HTTP Client (n8n webhook)
    implementation("org.springframework.boot:spring-boot-starter-webflux")

    // WebSocket (real-time alert push to frontend)
    implementation("org.springframework.boot:spring-boot-starter-websocket")

    // Elasticsearch (notification history storage)
    implementation("org.springframework.boot:spring-boot-starter-data-elasticsearch")

    // Test
    testImplementation("org.springframework.kafka:spring-kafka-test")
}

// Library module
tasks.named<Jar>("jar") {
    enabled = true
}
