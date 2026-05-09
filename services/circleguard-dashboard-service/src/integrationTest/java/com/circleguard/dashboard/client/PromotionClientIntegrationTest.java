package com.circleguard.dashboard.client;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@Tag("integration")
class PromotionClientIntegrationTest {

    @Autowired
    private PromotionClient promotionClient;

    @Test
    void getHealthStats_ReturnsMapWithTotalUsers() {
        Map<String, Object> stats = promotionClient.getHealthStats();

        assertNotNull(stats);
        assertFalse(stats.containsKey("error"), "No debe retornar error: " + stats.get("error"));
        assertTrue(stats.containsKey("totalUsers"));
        assertTrue(stats.containsKey("timestamp"));
    }

    @Test
    void getHealthStatsByDepartment_ReturnsDepartmentStats() {
        String department = "INGENIERIA";
        Map<String, Object> stats = promotionClient.getHealthStatsByDepartment(department);

        assertNotNull(stats);
        assertFalse(stats.containsKey("error"), "No debe retornar error: " + stats.get("error"));
        assertTrue(stats.containsKey("totalUsers"));
        assertEquals(department, stats.get("department"));
    }
}
