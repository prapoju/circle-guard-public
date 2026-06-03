package com.circleguard.auth.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.security.Key;
import java.util.Date;
import java.util.UUID;

@Service
public class QrTokenService {
    private final Key key;
    private final long expiration;
    private final MeterRegistry meterRegistry;

    public QrTokenService(@Value("${qr.secret}") String secret,
                         @Value("${qr.expiration:60000}") long expiration,
                         MeterRegistry meterRegistry) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
        this.expiration = expiration;
        this.meterRegistry = meterRegistry;
    }

    public String generateQrToken(UUID anonymousId) {
        meterRegistry.counter("circleguard.auth.qr.tokens.generated.total").increment();
        return Jwts.builder()
                .setSubject(anonymousId.toString())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }
}
