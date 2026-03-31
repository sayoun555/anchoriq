plugins {
    java
}

dependencies {
    // Internal modules
    implementation(project(":anchoriq-core"))

    // HTTP Client (OpenClaw API)
    implementation("org.springframework.boot:spring-boot-starter-webflux")

    // Neo4j (ontology queries - AI generates Cypher)
    implementation("org.springframework.boot:spring-boot-starter-data-neo4j")

    // Redis (AI result caching, risk score caching)
    implementation("org.springframework.boot:spring-boot-starter-data-redis")

    // Elasticsearch (AI decision log storage/search)
    implementation("org.springframework.boot:spring-boot-starter-data-elasticsearch")

    // PDF report generation
    implementation("com.github.librepdf:openpdf:${rootProject.extra["openpdfVersion"]}")

    // JSON processing (OpenClaw response parsing)
    implementation("com.fasterxml.jackson.core:jackson-databind")
}

// Library module
tasks.named<Jar>("jar") {
    enabled = true
}
