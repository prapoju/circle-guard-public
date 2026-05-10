FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY build/libs/circleguard-dashboard-service-*.jar app.jar

ENV SPRING_PROFILES_ACTIVE=docker \
    SERVER_PORT=8080

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
