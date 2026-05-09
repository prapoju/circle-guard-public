FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY build/libs/circleguard-identity-service-*.jar app.jar

ENV SPRING_PROFILES_ACTIVE=docker \
    SERVER_PORT=8083

EXPOSE 8083

ENTRYPOINT ["java", "-jar", "app.jar"]

# Run with: docker run --env-file identity-service.env -p 8083:8083 identity-service:latest
