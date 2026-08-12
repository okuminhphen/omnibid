FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY . .
RUN mvn -pl services/wallet-service -am clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/services/wallet-service/target/wallet-service-*.jar app.jar
EXPOSE 8081 9091
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
