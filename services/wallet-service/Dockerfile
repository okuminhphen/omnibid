FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /workspace

COPY pom.xml ./
COPY contracts/wallet-proto/pom.xml contracts/wallet-proto/pom.xml
COPY services/identity-service/pom.xml services/identity-service/pom.xml
COPY services/auction-service/pom.xml services/auction-service/pom.xml
COPY services/wallet-service/pom.xml services/wallet-service/pom.xml
COPY services/audit-service/pom.xml services/audit-service/pom.xml
RUN mvn --batch-mode --no-transfer-progress \
    -pl services/wallet-service -am \
    dependency:go-offline -DskipTests

COPY contracts/wallet-proto/src contracts/wallet-proto/src
COPY services/wallet-service/src services/wallet-service/src
RUN mvn --batch-mode --no-transfer-progress \
    -pl services/wallet-service -am \
    package -DskipTests

FROM eclipse-temurin:21-jre-jammy AS runtime

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system --gid 10001 omnibid \
    && useradd --system --uid 10001 --gid omnibid --home-dir /app omnibid

WORKDIR /app
COPY --from=build --chown=omnibid:omnibid \
    /workspace/services/wallet-service/target/wallet-service-*.jar /app/app.jar

USER 10001:10001
EXPOSE 8081 9091

ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/urandom"

HEALTHCHECK --interval=15s --timeout=5s --start-period=30s --retries=5 \
    CMD curl --fail --silent --show-error http://127.0.0.1:8081/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
