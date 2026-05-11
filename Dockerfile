FROM maven:3.9.6-eclipse-temurin-17 AS builder

WORKDIR /build

# Copy pom.xml first for dependency caching — only re-runs if pom.xml changes
COPY pom.xml .
RUN mvn dependency:go-offline -q

COPY src ./src
RUN mvn clean package -DskipTests -q

# Use JRE-only (not JDK) for the runtime — saves ~100 MB
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app
COPY --from=builder /build/target/tool-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-Xmx512m", "-jar", "app.jar"]
