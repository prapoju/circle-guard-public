package com.circleguard.dashboard.service;

import com.circleguard.dashboard.client.PromotionClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    private JdbcTemplate jdbc;

    @Mock
    private PromotionClient promotionClient;

    @Mock
    private KAnonymityFilter kAnonymityFilter;

    @InjectMocks
    private AnalyticsService service;

    @Test
    void testGetTimeSeries_FallsBackToMockData() {
        when(jdbc.queryForList(anyString(), anyInt()))
                .thenThrow(new org.springframework.dao.DataAccessResourceFailureException("Table not found"));

        List<Map<String, Object>> result = service.getTimeSeries("hourly", 10);

        assertNotNull(result);
        assertFalse(result.isEmpty());
        verify(jdbc).queryForList(anyString(), eq(10));
    }
}