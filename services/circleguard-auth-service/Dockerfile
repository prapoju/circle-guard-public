FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY build/libs/circleguard-auth-service-*.jar app.jar

ENV SPRING_PROFILES_ACTIVE=docker \
    SERVER_PORT=8180

EXPOSE 8180

ENTRYPOINT ["java", "-jar", "app.jar"]
