package com.log_collector.service;

import com.log_collector.model.LogEvent;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Set;

@Service
public class LogProcessor {

    private final WebClient webClient;
    private final Set<String> blacklist = Set.of("root", "admin");

    public LogProcessor(WebClient webClient) {
        this.webClient = webClient;
    }

    public Mono<Void> process(String raw) {

        LogEvent log = new LogEvent();
        log.setTimestamp(Instant.now());
        log.setUsername(raw.contains("root") ? "root" : "user");
        log.setSeverity("INFO");
        log.setBlacklisted(blacklist.contains(log.getUsername()));
        log.setRawMessage(raw);

        return webClient.post()
                .uri("/ingest")
                .bodyValue(log)
                .retrieve()
                .bodyToMono(Void.class);
    }
}
