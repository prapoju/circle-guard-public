package com.circleguard.notification.service;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Tag("integration")
class LmsServiceIntegrationTest {

    @Autowired
    private LmsService lmsService;

    @Test
    void syncRemoteAttendance_completesWithoutError() throws Exception {
        CompletableFuture<Void> future = lmsService.syncRemoteAttendance(
                "550e8400-e29b-41d4-a716-446655440000",
                "SUSPECT"
        );

        assertDoesNotThrow(() -> future.get(5, TimeUnit.SECONDS));
        assertTrue(future.isDone());
        assertFalse(future.isCompletedExceptionally());
    }
}
