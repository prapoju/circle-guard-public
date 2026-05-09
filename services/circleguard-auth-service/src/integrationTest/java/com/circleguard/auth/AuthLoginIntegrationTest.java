package com.circleguard.auth;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@Tag("integration")
class AuthLoginIntegrationTest {

    @Value("${auth.service.base-url}")
    private String baseUrl;

    @Value("${identity.service.url}")
    private String identityServiceUrl;

    private final TestRestTemplate restTemplate = new TestRestTemplate();

    @Test
    void testLogin_ReturnsJwtWithAnonymousId() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> body = Map.of(
            "username", "super_admin",
            "password", "password"
        );

        HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(
            baseUrl + "/api/v1/auth/login",
            request,
            Map.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().get("token"));
        assertNotNull(response.getBody().get("anonymousId"));
    }
}
