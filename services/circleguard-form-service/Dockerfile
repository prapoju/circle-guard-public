FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY build/libs/circleguard-form-service-*.jar app.jar

ENV SPRING_PROFILES_ACTIVE=docker \
    SERVER_PORT=8086

EXPOSE 8086

ENTRYPOINT ["java", "-jar", "app.jar"]
