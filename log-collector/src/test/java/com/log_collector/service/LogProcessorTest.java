package com.log_collector.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class LogProcessorTest {

    private WebClient webClient;
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;
    private WebClient.RequestHeadersSpec<?> requestHeadersSpec;
    private WebClient.ResponseSpec responseSpec;

    private LogProcessor logProcessor;

    @BeforeEach
    void setUp() {

        webClient = mock(WebClient.class);
        requestBodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        requestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        responseSpec = mock(WebClient.ResponseSpec.class);

        // FULL chain mocking
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.bodyValue(any()))
                .thenReturn((WebClient.RequestHeadersSpec) requestHeadersSpec);


        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(Void.class))
                .thenReturn(Mono.empty());

        logProcessor = new LogProcessor(webClient);
    }

    @Test
    void process_shouldInvokeBodyToMono() {

        // Act
        logProcessor.process("root login event").block();

        // Assert (coverage only)
        verify(responseSpec, times(1))
                .bodyToMono(Void.class);
    }
}
