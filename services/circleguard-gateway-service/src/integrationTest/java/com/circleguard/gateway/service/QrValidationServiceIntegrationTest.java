package com.circleguard.gateway.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.security.Key;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifica QrValidationService contra una instancia real de Redis levantada
 * con Testcontainers. Cubre los tres caminos: token valido con status seguro,
 * token valido con status de riesgo en Redis, y token invalido.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
class QrValidationServiceIntegrationTest {

    private static final String QR_SECRET = "my-qr-secret-key-for-dev-1234567890";

    @Container
    static final GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7.2"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        registry.add("qr.secret", () -> QR_SECRET);
    }

    @Autowired
    private QrValidationService service;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @BeforeEach
    void cleanRedis() {
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
    }

    @Test
    void grantsAccessWhenTokenIsValidAndStatusIsSafe() {
        String anonymousId = "user-safe-123";
        String token = generateToken(anonymousId);
        redisTemplate.opsForValue().set("user:status:" + anonymousId, "ACTIVE");

        QrValidationService.ValidationResult result = service.validateToken(token);

        assertThat(result.valid()).isTrue();
        assertThat(result.status()).isEqualTo("GREEN");
    }

    @Test
    void deniesAccessWhenStatusIsContagied() {
        String anonymousId = "user-risk-456";
        String token = generateToken(anonymousId);
        redisTemplate.opsForValue().set("user:status:" + anonymousId, "CONTAGIED");

        QrValidationService.ValidationResult result = service.validateToken(token);

        assertThat(result.valid()).isFalse();
        assertThat(result.status()).isEqualTo("RED");
        assertThat(result.message()).contains("Health Risk");
    }

    @Test
    void deniesAccessWhenTokenIsInvalid() {
        QrValidationService.ValidationResult result = service.validateToken("not-a-valid-jwt");

        assertThat(result.valid()).isFalse();
        assertThat(result.status()).isEqualTo("RED");
        assertThat(result.message()).contains("Invalid");
    }

    private String generateToken(String anonymousId) {
        Key key = Keys.hmacShaKeyFor(QR_SECRET.getBytes());
        return Jwts.builder()
                .setSubject(anonymousId)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 300_000))
                .signWith(key)
                .compact();
    }
}
