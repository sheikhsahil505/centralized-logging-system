package com.log_server.model;

import org.junit.jupiter.api.Test;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class LogEventTest {

    @Test
    void testGettersAndSetters() {
        LogEvent log = new LogEvent();
        Instant now = Instant.now();

        log.setTimestamp(now);
        log.setService("auth-service");
        log.setEventCategory("authentication");
        log.setSeverity("INFO");
        log.setUsername("john");
        log.setHostname("server-01");
        log.setRawMessage("User logged in");
        log.setBlacklisted(true);

        assertEquals(now, log.getTimestamp());
        assertEquals("auth-service", log.getService());
        assertEquals("authentication", log.getEventCategory());
        assertEquals("INFO", log.getSeverity());
        assertEquals("john", log.getUsername());
        assertEquals("server-01", log.getHostname());
        assertEquals("User logged in", log.getRawMessage());
        assertTrue(log.isBlacklisted());
    }

    @Test
    void testEqualsAndHashCode() {
        LogEvent log1 = new LogEvent();
        LogEvent log2 = new LogEvent();
        Instant now = Instant.now();

        log1.setTimestamp(now);
        log1.setService("service");
        log2.setTimestamp(now);
        log2.setService("service");

        assertEquals(log1, log2);
        assertEquals(log1.hashCode(), log2.hashCode());
    }

    @Test
    void testToString() {
        LogEvent log = new LogEvent();
        log.setService("test-service");

        String result = log.toString();

        assertNotNull(result);
        assertTrue(result.contains("test-service"));
    }
}