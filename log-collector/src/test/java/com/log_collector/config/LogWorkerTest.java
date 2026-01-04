package com.log_collector.config;

import com.log_collector.model.LogEvent;
import com.log_collector.service.LogParser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class LogWorkerTest {

    private BlockingQueue<String> queue;
    private ExecutorService executorService;
    private LogParser parser;
    private WebClient.Builder webClientBuilder;
    private WebClient webClient;
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;
    private WebClient.RequestBodySpec requestBodySpec;
    private WebClient.ResponseSpec responseSpec;

    @BeforeEach
    void setUp() {
        queue = new LinkedBlockingQueue<>();
        executorService = Executors.newFixedThreadPool(4);
        parser = mock(LogParser.class);

        // Mock WebClient chain
        webClientBuilder = mock(WebClient.Builder.class);
        webClient = mock(WebClient.class);
        requestBodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        requestBodySpec = mock(WebClient.RequestBodySpec.class);
        responseSpec = mock(WebClient.ResponseSpec.class);

        when(webClientBuilder.baseUrl(anyString())).thenReturn(webClientBuilder);
        when(webClientBuilder.build()).thenReturn(webClient);
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodyUriSpec.bodyValue(any()))
                .thenReturn((WebClient.RequestHeadersSpec) requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(Void.class)).thenReturn(Mono.empty());
    }

    @AfterEach
    void tearDown() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdownNow();
            try {
                executorService.awaitTermination(1, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Test
    void testConstructorBuildsWebClient() {
        new LogWorker(queue, executorService, parser, webClientBuilder);

        verify(webClientBuilder).baseUrl("http://localhost:8082");
        verify(webClientBuilder).build();
    }

    @Test
    void testConsumeProcessesLogEvent() throws Exception {
        LogEvent mockEvent = createLogEvent();
        when(parser.parse("test log")).thenReturn(mockEvent);

        new LogWorker(queue, executorService, parser, webClientBuilder);

        queue.put("test log");

        Thread.sleep(500);

        verify(parser, timeout(1000)).parse("test log");
        verify(webClient, timeout(1000)).post();
        verify(requestBodyUriSpec, timeout(1000)).uri("/ingest");
        verify(requestBodySpec, timeout(1000)).bodyValue(mockEvent);
    }

    @Test
    void testConsumeHandlesMultipleMessages() throws Exception {
        LogEvent event1 = createLogEvent();
        LogEvent event2 = createLogEvent();

        when(parser.parse("log1")).thenReturn(event1);
        when(parser.parse("log2")).thenReturn(event2);

        new LogWorker(queue, executorService, parser, webClientBuilder);

        queue.put("log1");
        queue.put("log2");

        Thread.sleep(500);

        verify(parser, timeout(1000)).parse("log1");
        verify(parser, timeout(1000)).parse("log2");
        verify(webClient, timeout(1000).times(2)).post();
    }

    @Test
    void testConsumeHandlesParsingException() throws Exception {
        when(parser.parse("bad log")).thenThrow(new RuntimeException("Parse error"));

        LogEvent goodEvent = createLogEvent();
        when(parser.parse("good log")).thenReturn(goodEvent);

        new LogWorker(queue, executorService, parser, webClientBuilder);

        queue.put("bad log");
        queue.put("good log");

        Thread.sleep(500);

        verify(parser, timeout(1000)).parse("bad log");
        verify(parser, timeout(1000)).parse("good log");
        verify(webClient, timeout(1000).atLeast(1)).post();
    }

    @Test
    void testConsumeHandlesWebClientException() throws Exception {
        LogEvent event = createLogEvent();
        when(parser.parse("test log")).thenReturn(event);
        when(responseSpec.bodyToMono(Void.class)).thenReturn(Mono.error(new RuntimeException("Network error")));

        new LogWorker(queue, executorService, parser, webClientBuilder);

        queue.put("test log");

        Thread.sleep(500);

        verify(parser, timeout(1000)).parse("test log");
        verify(webClient, timeout(1000)).post();
    }

    @Test
    void testWebClientBaseUrlConfiguration() {
        new LogWorker(queue, executorService, parser, webClientBuilder);

        verify(webClientBuilder).baseUrl("http://localhost:8082");
    }

    @Test
    void testPostRequestToIngestEndpoint() throws Exception {
        LogEvent event = createLogEvent();
        when(parser.parse("test")).thenReturn(event);

        new LogWorker(queue, executorService, parser, webClientBuilder);

        queue.put("test");

        Thread.sleep(500);

        verify(requestBodyUriSpec, timeout(1000)).uri("/ingest");
        verify(requestBodySpec, timeout(1000)).bodyValue(event);
    }

    @Test
    void testQueueBlockingBehavior() throws Exception {
        LogEvent event = createLogEvent();
        when(parser.parse(anyString())).thenReturn(event);

        new LogWorker(queue, executorService, parser, webClientBuilder);

        assertTrue(queue.isEmpty());

        queue.put("test");

        Thread.sleep(300);

        verify(parser, timeout(1000)).parse("test");
    }

    @Test
    void testMultipleWorkersProcessConcurrently() throws Exception {
        LogEvent event = createLogEvent();
        when(parser.parse(anyString())).thenReturn(event);

        new LogWorker(queue, executorService, parser, webClientBuilder);

        for (int i = 0; i < 10; i++) {
            queue.put("log" + i);
        }

        Thread.sleep(1000);

        verify(parser, timeout(2000).atLeast(10)).parse(anyString());
    }

    @Test
    void testParsedEventIsSentToWebClient() throws Exception {
        LogEvent event = createLogEvent();
        event.setService("custom-service");
        event.setSeverity("ERROR");

        when(parser.parse("test")).thenReturn(event);

        new LogWorker(queue, executorService, parser, webClientBuilder);

        queue.put("test");

        Thread.sleep(500);

        verify(requestBodySpec, timeout(1000)).bodyValue(event);
        verify(responseSpec, timeout(1000)).bodyToMono(Void.class);
    }

    private LogEvent createLogEvent() {
        LogEvent event = new LogEvent();
        event.setTimestamp(Instant.now());
        event.setService("test-service");
        event.setSeverity("INFO");
        event.setEventCategory("test");
        event.setUsername("user");
        event.setHostname("host");
        event.setRawMessage("message");
        event.setBlacklisted(false);
        return event;
    }
}