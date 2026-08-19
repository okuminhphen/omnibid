FROM eclipse-temurin:21-jre
ARG JAR_FILE=target/identity-service-0.1.0-SNAPSHOT.jar
COPY ${JAR_FILE} app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "/app.jar"]
