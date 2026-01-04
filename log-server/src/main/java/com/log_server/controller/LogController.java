package com.log_server.controller;

import com.log_server.model.LogEvent;
import com.log_server.service.LogStore;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Comparator;

@RestController
@RequestMapping
public class LogController {

    private final LogStore store;

    public LogController(LogStore store) {
        this.store = store;
    }

    // -------------------- Ingest API --------------------
    @PostMapping("/ingest")
    public Mono<Void> ingest(@RequestBody Mono<LogEvent> log) {
        return log.doOnNext(store::add).then();
    }

    // -------------------- Query API --------------------
    @GetMapping("/logs")
    public Flux<LogEvent> getLogs(
            @RequestParam(name = "service", required = false) String service,
            @RequestParam(name = "level", required = false) String level,
            @RequestParam(name = "username", required = false) String username,
            @RequestParam(name = "isBlacklisted", required = false) Boolean isBlacklisted,
            @RequestParam(name = "limit", required = false, defaultValue = "50") Integer limit,
            @RequestParam(name = "sort", required = false, defaultValue = "timestamp") String sort
    ) {

        Flux<LogEvent> flux = Flux.fromIterable(store.getAll())
                .filter(log -> service == null || service.equalsIgnoreCase(log.getService()))
                .filter(log -> level == null || level.equalsIgnoreCase(log.getSeverity()))
                .filter(log -> username == null || username.equals(log.getUsername()))
                .filter(log -> isBlacklisted == null || isBlacklisted.equals(log.isBlacklisted()));

        if ("timestamp".equalsIgnoreCase(sort)) {
            flux = flux.sort(Comparator.comparing(LogEvent::getTimestamp));
        }

        return flux.take(limit);
    }
}
