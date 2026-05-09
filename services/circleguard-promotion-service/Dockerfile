FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY build/libs/circleguard-promotion-service-*.jar app.jar

ENV SPRING_PROFILES_ACTIVE=docker \
    SERVER_PORT=8088

EXPOSE 8088

ENTRYPOINT ["java", "-jar", "app.jar"]
