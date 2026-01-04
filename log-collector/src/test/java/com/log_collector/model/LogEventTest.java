package com.log_collector.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class LogEventTest {

    private LogEvent logEvent;

    @BeforeEach
    void setUp() {
        logEvent = new LogEvent();
    }

    @Test
    void testTimestamp() {
        Instant now = Instant.now();
        logEvent.setTimestamp(now);
        assertEquals(now, logEvent.getTimestamp());
    }

    @Test
    void testService() {
        logEvent.setService("linux_login");
        assertEquals("linux_login", logEvent.getService());
    }

    @Test
    void testEventCategory() {
        logEvent.setEventCategory("login.audit");
        assertEquals("login.audit", logEvent.getEventCategory());
    }

    @Test
    void testSeverity() {
        logEvent.setSeverity("INFO");
        assertEquals("INFO", logEvent.getSeverity());
    }

    @Test
    void testUsername() {
        logEvent.setUsername("testuser");
        assertEquals("testuser", logEvent.getUsername());
    }

    @Test
    void testHostname() {
        logEvent.setHostname("server01");
        assertEquals("server01", logEvent.getHostname());
    }

    @Test
    void testRawMessage() {
        String msg = "test log message";
        logEvent.setRawMessage(msg);
        assertEquals(msg, logEvent.getRawMessage());
    }

    @Test
    void testBlacklisted() {
        logEvent.setBlacklisted(true);
        assertTrue(logEvent.isBlacklisted());

        logEvent.setBlacklisted(false);
        assertFalse(logEvent.isBlacklisted());
    }

    @Test
    void testAllFieldsTogether() {
        Instant now = Instant.now();
        logEvent.setTimestamp(now);
        logEvent.setService("windows_login");
        logEvent.setEventCategory("login.audit");
        logEvent.setSeverity("WARN");
        logEvent.setUsername("admin");
        logEvent.setHostname("WIN-PC");
        logEvent.setRawMessage("Full log message");
        logEvent.setBlacklisted(true);

        assertEquals(now, logEvent.getTimestamp());
        assertEquals("windows_login", logEvent.getService());
        assertEquals("login.audit", logEvent.getEventCategory());
        assertEquals("WARN", logEvent.getSeverity());
        assertEquals("admin", logEvent.getUsername());
        assertEquals("WIN-PC", logEvent.getHostname());
        assertEquals("Full log message", logEvent.getRawMessage());
        assertTrue(logEvent.isBlacklisted());
    }

    @Test
    void testToString() {
        logEvent.setUsername("testuser");
        logEvent.setService("linux_login");

        String str = logEvent.toString();
        assertNotNull(str);
        assertTrue(str.contains("testuser") || str.contains("LogEvent"));
    }

    @Test
    void testEqualsAndHashCode() {
        LogEvent event1 = new LogEvent();
        event1.setUsername("user1");
        event1.setService("service1");

        LogEvent event2 = new LogEvent();
        event2.setUsername("user1");
        event2.setService("service1");

        assertEquals(event1, event2);
        assertEquals(event1.hashCode(), event2.hashCode());
    }
}