plugins {
    java
    id("org.springframework.boot") version "3.4.0"
    id("io.spring.dependency-management") version "1.1.6"
}

allprojects {
    group = "com.anchoriq"
    version = "0.0.1-SNAPSHOT"
}

extra["jjwtVersion"] = "0.12.6"
extra["mapstructVersion"] = "1.6.3"
extra["playwrightVersion"] = "1.49.0"
extra["openpdfVersion"] = "2.0.3"
extra["opencsvVersion"] = "5.9"
extra["springdocVersion"] = "2.7.0"
extra["stripeVersion"] = "28.2.0"
extra["testcontainersVersion"] = "1.20.4"

subprojects {
    apply(plugin = "java")
    apply(plugin = "io.spring.dependency-management")

    java {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    repositories {
        mavenCentral()
    }

    dependencyManagement {
        imports {
            mavenBom("org.springframework.boot:spring-boot-dependencies:3.4.0")
        }
    }

    dependencies {
        // Common - all modules
        compileOnly("org.projectlombok:lombok")
        annotationProcessor("org.projectlombok:lombok")

        // Test common
        testImplementation("org.springframework.boot:spring-boot-starter-test")
        testImplementation("org.assertj:assertj-core")
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }
}

// Root project should not produce a bootable jar
tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    enabled = false
}

tasks.named<Jar>("jar") {
    enabled = true
}
