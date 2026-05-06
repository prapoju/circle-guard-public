package com.circleguard.auth.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class QrTokenServiceTest {

    private static final String SECRET = "test-secret-key-that-is-at-least-256-bits-long-for-hmac-sha";
    private static final long EXPIRATION = 60000L;
    private QrTokenService qrTokenService;

    @BeforeEach
    void setUp() {
        qrTokenService = new QrTokenService(SECRET, EXPIRATION);
    }

    @Test
    void testQrToken_ContainsCorrectSubject() {
        UUID anonymousId = UUID.randomUUID();

        String token = qrTokenService.generateQrToken(anonymousId);

        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        var claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();

        assertEquals(anonymousId.toString(), claims.getSubject());
    }

    @Test
    void testQrToken_ExpiresCorrectly() {
        UUID anonymousId = UUID.randomUUID();
        
        String token = qrTokenService.generateQrToken(anonymousId);
        
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        var claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
        
        long now = System.currentTimeMillis();
        long expirationTime = claims.getExpiration().getTime();
        long expectedExpiration = now + EXPIRATION;
        
        assertTrue(Math.abs(expirationTime - expectedExpiration) < 5000,
                "Expiration should be ~60000ms from now, but was: " + (expirationTime - now));
    }
}