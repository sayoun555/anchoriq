FROM eclipse-temurin:21-jdk AS builder

WORKDIR /app
COPY . .
RUN ./gradlew :anchoriq-api:bootJar --no-daemon -x test

FROM eclipse-temurin:21-jre

WORKDIR /app
COPY --from=builder /app/anchoriq-api/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
