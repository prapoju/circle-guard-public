FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY build/libs/circleguard-notification-service-*.jar app.jar

ENV SPRING_PROFILES_ACTIVE=docker \
    SERVER_PORT=8082

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "app.jar"]
