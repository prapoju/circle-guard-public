package com.circleguard.dashboard.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Component
@Slf4j
public class PromotionClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${promotion.service.url}")
    private String promotionServiceUrl;

    @SuppressWarnings("unchecked")
    @CircuitBreaker(name = "promotionService", fallbackMethod = "getHealthStatsFallback")
    public Map<String, Object> getHealthStats() {
        return restTemplate.getForObject(
                promotionServiceUrl + "/api/v1/health-status/stats",
                Map.class
        );
    }

    @SuppressWarnings("unchecked")
    @CircuitBreaker(name = "promotionService", fallbackMethod = "getHealthStatsByDepartmentFallback")
    public Map<String, Object> getHealthStatsByDepartment(String department) {
        return restTemplate.getForObject(
                promotionServiceUrl + "/api/v1/health-status/stats/department/" + department,
                Map.class
        );
    }

    @SuppressWarnings("unused")
    private Map<String, Object> getHealthStatsFallback(Throwable t) {
        log.error("Circuit breaker fallback: promotion-service unavailable for global stats", t);
        return Map.of("error", "Service unavailable", "timestamp", new Date());
    }

    @SuppressWarnings("unused")
    private Map<String, Object> getHealthStatsByDepartmentFallback(String department, Throwable t) {
        log.error("Circuit breaker fallback: promotion-service unavailable for department: {}", department, t);
        return Map.of("error", "Service unavailable", "department", department, "timestamp", new Date());
    }
}
