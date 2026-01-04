package com.log_server.controller;

import com.log_server.model.LogEvent;
import com.log_server.service.LogStore;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
public class MetricsController {

    private final LogStore logStore;

    public MetricsController(LogStore logStore) {
        this.logStore = logStore;
    }

    @GetMapping("/metrics")
    public Mono<Map<String, Object>> metrics() {

        List<LogEvent> logs = logStore.getAll();

        Map<String, Long> byCategory =
                logs.stream()
                        .collect(Collectors.groupingBy(
                                LogEvent::getEventCategory,
                                Collectors.counting()
                        ));

        Map<String, Long> bySeverity =
                logs.stream()
                        .collect(Collectors.groupingBy(
                                LogEvent::getSeverity,
                                Collectors.counting()
                        ));

        Map<String, Object> metrics = Map.of(
                "totalLogs", logs.size(),
                "logsByCategory", byCategory,
                "logsBySeverity", bySeverity
        );

        return Mono.just(metrics);
    }
}
