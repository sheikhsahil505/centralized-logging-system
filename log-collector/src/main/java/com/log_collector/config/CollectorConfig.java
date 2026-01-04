package com.log_collector.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class CollectorConfig {

    @Bean
    public BlockingQueue<String> logQueue() {
        return new ArrayBlockingQueue<>(1000); // backpressure
    }

    @Bean(destroyMethod = "shutdown")
    public ExecutorService workerPool() {
        return Executors.newFixedThreadPool(4);
    }
}
