package com.log_server.controller;

import com.log_server.model.LogEvent;
import com.log_server.service.LogStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.reactive.server.WebTestClient;

@WebFluxTest(controllers = LogController.class)
@Import(LogStore.class)
class LogControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private LogStore logStore;

    @BeforeEach
    void setup() {
        logStore.getAll().clear();
    }

    @Test
    void shouldIngestLog() {
        LogEvent log = new LogEvent();
//        log.setTimestamp("2026-01-04T10:00:00Z");
        log.setUsername("root");
        log.setSeverity("INFO");
        log.setBlacklisted(true);
        log.setRawMessage("test log");

        webTestClient.post()
                .uri("/ingest")
                .bodyValue(log)
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void shouldReturnLogs() {
        LogEvent log = new LogEvent();
//        log.setTimestamp("2026-01-04T10:00:00Z");
        log.setUsername("user");
        log.setSeverity("INFO");
        log.setBlacklisted(false);
        log.setRawMessage("test log");

        logStore.add(log);

        webTestClient.get()
                .uri("/logs")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].username").isEqualTo("user");
    }

    @Test
    void shouldFilterByBlacklisted() {
        LogEvent log1 = new LogEvent();
        log1.setUsername("root");
        log1.setBlacklisted(true);

        LogEvent log2 = new LogEvent();
        log2.setUsername("user");
        log2.setBlacklisted(false);

        logStore.add(log1);
        logStore.add(log2);

        webTestClient.get()
                .uri("/logs?isBlacklisted=true")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(1);
    }
}