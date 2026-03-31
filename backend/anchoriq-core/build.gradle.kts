plugins {
    java
}

dependencies {
    // JPA - Entity annotations (@Entity, @Id, @Version)
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")

    // Validation - Domain validation (@NotNull, @Size)
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // MapStruct - DTO mapping (VO conversions within domain)
    implementation("org.mapstruct:mapstruct:${rootProject.extra["mapstructVersion"]}")
    annotationProcessor("org.mapstruct:mapstruct-processor:${rootProject.extra["mapstructVersion"]}")
}

// Core module is a library, not a bootable application
tasks.named<Jar>("jar") {
    enabled = true
}
