FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY build/libs/circleguard-gateway-service-*.jar app.jar

ENV SPRING_PROFILES_ACTIVE=docker \
    SERVER_PORT=8087

EXPOSE 8087

ENTRYPOINT ["java", "-jar", "app.jar"]
