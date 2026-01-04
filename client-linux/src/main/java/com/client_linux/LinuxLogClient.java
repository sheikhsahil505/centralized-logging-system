package com.client_linux;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class LinuxLogClient {

    private static final Logger log = LoggerFactory.getLogger(LinuxLogClient.class);

    private static final String COLLECTOR_HOST = "localhost";
    private static final int COLLECTOR_PORT = 9000;
    private static final int SEND_INTERVAL_SECONDS = 2;
    private static final int RECONNECT_DELAY_SECONDS = 1;

    private static final String[] LOG_MESSAGES = {
            // Login Audit
            "<86> aiops9242 sudo: pam_unix(sudo:session): session opened for user root(uid=0)",

            // Syslog / Logout Audit
            "<34> aiops9242 cron: pam_unix(cron:session): session closed for user root"
    };

    private final AtomicInteger index = new AtomicInteger(0);

    private Socket socket;
    private OutputStream outputStream;
    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor();

    public static void main(String[] args) {
        new LinuxLogClient().start();
    }

    public void start() {
        connect();
        scheduleLogSending();
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
    }

    private void scheduleLogSending() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                sendLog(buildLogMessage());
            } catch (IOException ex) {
                log.warn("Connection lost. Reconnecting...");
                reconnect();
            }
        }, 0, SEND_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    void connect() {
        try {
            socket = new Socket(COLLECTOR_HOST, COLLECTOR_PORT);
            outputStream = socket.getOutputStream();
            log.info("Connected to Log Collector at {}:{}", COLLECTOR_HOST, COLLECTOR_PORT);
        } catch (IOException ex) {
            log.error("Unable to connect to Log Collector. Will retry...");
        }
    }

    private void reconnect() {
        closeResources();
        sleep(RECONNECT_DELAY_SECONDS);
        connect();
    }

    private void sendLog(String message) throws IOException {
        if (outputStream == null) {
            throw new IOException("Output stream not available");
        }
        outputStream.write(message.getBytes(StandardCharsets.UTF_8));
        outputStream.flush();
    }

    private String buildLogMessage() {
        int i = index.getAndIncrement() % LOG_MESSAGES.length;
        return "{\"message\":\"" + LOG_MESSAGES[i] + "\"}\n";
    }

    private void shutdown() {
        log.info("Shutting down Linux Log Client...");
        scheduler.shutdownNow();
        closeResources();
    }

    private void closeResources() {
        try {
            if (socket != null) socket.close();
        } catch (IOException ignored) {
        }
    }

    private void sleep(int seconds) {
        try {
            TimeUnit.SECONDS.sleep(seconds);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }
}
