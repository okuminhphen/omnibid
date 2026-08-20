FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /workspace

COPY pom.xml ./
COPY contracts/wallet-proto/pom.xml contracts/wallet-proto/pom.xml
COPY services/identity-service/pom.xml services/identity-service/pom.xml
COPY services/auction-service/pom.xml services/auction-service/pom.xml
COPY services/wallet-service/pom.xml services/wallet-service/pom.xml
COPY services/audit-service/pom.xml services/audit-service/pom.xml
RUN mvn --batch-mode --no-transfer-progress \
    -pl services/audit-service -am \
    dependency:go-offline -DskipTests

COPY services/audit-service/src services/audit-service/src
RUN mvn --batch-mode --no-transfer-progress \
    -pl services/audit-service -am \
    package -DskipTests

FROM eclipse-temurin:21-jre-jammy AS runtime

RUN groupadd --system --gid 10001 omnibid \
    && useradd --system --uid 10001 --gid omnibid --home-dir /app omnibid

WORKDIR /app
COPY --from=build --chown=omnibid:omnibid \
    /workspace/services/audit-service/target/audit-service-*.jar /app/app.jar

USER 10001:10001

ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/urandom"

# Audit is intentionally a headless Kafka consumer. Its dependency readiness is
# handled by Compose; the container probe verifies that the JVM remains alive.
HEALTHCHECK --interval=15s --timeout=5s --start-period=30s --retries=5 \
    CMD ["/bin/sh", "-c", "kill -0 1"]

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
