package com.circleguard.auth.client;

import com.circleguard.auth.exception.IdentityServiceUnavailableException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import java.util.*;

@Component
public class IdentityClient {
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${identity.service.url}")
    private String identityServiceUrl;

    @CircuitBreaker(name = "identityService", fallbackMethod = "getAnonymousIdFallback")
    public UUID getAnonymousId(String realIdentity) {
        Map<String, String> request = Map.of("realIdentity", realIdentity);
        Map response = restTemplate.postForObject(
            identityServiceUrl + "/api/v1/identities/map",
            request,
            Map.class
        );
        return UUID.fromString(response.get("anonymousId").toString());
    }

    @SuppressWarnings("unused")
    private UUID getAnonymousIdFallback(String realIdentity, Throwable t) {
        throw new IdentityServiceUnavailableException(
            "identity-service unavailable (circuit breaker is open or call failed)", t);
    }
}
