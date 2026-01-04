package com.log_server.controller;

import com.log_server.model.LogEvent;
import com.log_server.service.LogStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MetricsControllerTest {

    private LogStore logStore;
    private MetricsController metricsController;

    @BeforeEach
    void setUp() {
        logStore = new LogStore(); // REAL OBJECT (NO MOCK)
        metricsController = new MetricsController(logStore);
    }

    @Test
    void metrics_shouldReturnCorrectCounts() {

        LogEvent log1 = new LogEvent();
        log1.setEventCategory("login.audit");
        log1.setSeverity("INFO");

        LogEvent log2 = new LogEvent();
        log2.setEventCategory("login.audit");
        log2.setSeverity("INFO");

        LogEvent log3 = new LogEvent();
        log3.setEventCategory("logout.audit");
        log3.setSeverity("WARN");

        logStore.add(log1);
        logStore.add(log2);
        logStore.add(log3);

        Mono<Map<String, Object>> result = metricsController.metrics();
        Map<String, Object> metrics = result.block();

        assertNotNull(metrics);
        assertEquals(3, metrics.get("totalLogs"));

        Map<String, Long> byCategory =
                (Map<String, Long>) metrics.get("logsByCategory");
        Map<String, Long> bySeverity =
                (Map<String, Long>) metrics.get("logsBySeverity");

        assertEquals(2L, byCategory.get("login.audit"));
        assertEquals(1L, byCategory.get("logout.audit"));

        assertEquals(2L, bySeverity.get("INFO"));
        assertEquals(1L, bySeverity.get("WARN"));
    }

    @Test
    void metrics_shouldHandleEmptyLogs() {

        Map<String, Object> metrics = metricsController.metrics().block();

        assertNotNull(metrics);
        assertEquals(0, metrics.get("totalLogs"));

        Map<String, Long> byCategory =
                (Map<String, Long>) metrics.get("logsByCategory");
        Map<String, Long> bySeverity =
                (Map<String, Long>) metrics.get("logsBySeverity");

        assertTrue(byCategory.isEmpty());
        assertTrue(bySeverity.isEmpty());
    }
}
