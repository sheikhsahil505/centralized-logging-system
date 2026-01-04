package com.log_collector.config;

import com.log_collector.model.LogEvent;
import com.log_collector.service.LogParser;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;

@Component
public class LogWorker {

    private final BlockingQueue<String> queue;
    private final ExecutorService workers;
    private final LogParser parser;
    private final WebClient webClient;

    public LogWorker(
            BlockingQueue<String> queue,
            ExecutorService workers,
            LogParser parser,
            WebClient.Builder builder
    ) {
        this.queue = queue;
        this.workers = workers;
        this.parser = parser;
        this.webClient = builder.baseUrl("http://localhost:8082").build();

        startWorkers();
    }

    private void startWorkers() {
        for (int i = 0; i < 4; i++) {
            workers.submit(this::consume);
        }
    }

    private void consume() {
        while (true) {
            try {
                String raw = queue.take(); // blocks safely
                LogEvent event = parser.parse(raw);

                webClient.post()
                        .uri("/ingest")
                        .bodyValue(event)
                        .retrieve()
                        .bodyToMono(Void.class)
                        .subscribe();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
