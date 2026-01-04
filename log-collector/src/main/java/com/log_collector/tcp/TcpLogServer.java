package com.log_collector.tcp;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;
import reactor.netty.DisposableServer;
import reactor.netty.tcp.TcpServer;

import java.util.concurrent.BlockingQueue;

@Component
public class TcpLogServer {

    private final BlockingQueue<String> queue;
    private DisposableServer server;

    public TcpLogServer(BlockingQueue<String> queue) {
        this.queue = queue;
    }

    @PostConstruct
    public void start() {
        server = TcpServer.create()
                .host("0.0.0.0")
                .port(9000)
                .handle((in, out) ->
                        in.receive()
                                .asString()
                                .doOnNext(msg -> {
                                    if (!queue.offer(msg)) {
                                        System.err.println("Queue full. Dropping log");
                                    }
                                })
                                .then()
                )
                .bindNow();
    }

    @PreDestroy
    public void stop() {
        if (server != null) server.disposeNow();
    }
}
