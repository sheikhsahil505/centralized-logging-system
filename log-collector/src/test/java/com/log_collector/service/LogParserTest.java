package com.log_collector.service;

import com.log_collector.model.LogEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LogParserTest {

    private LogParser logParser;

    @BeforeEach
    void setUp() {
        logParser = new LogParser();
    }


    @Test
    void testParse_LinuxLogout_PlainText() {
        // Not a valid JSON string triggers the catch block
        String raw = "host02 user alice session closed";
        LogEvent event = logParser.parse(raw);

        assertEquals(raw, event.getRawMessage());
        assertEquals("alice", event.getUsername());
        assertFalse(event.isBlacklisted());
        assertEquals("linux_logout", event.getService());
    }

    /**
     * Coverage: extractUsername (Windows pattern),
     * classifyEvent (windows_login).
     */
    @Test
    void testParse_WindowsLogin() {
        String raw = "win-srv Microsoft-Windows-Security-Auditing Account Name: bob";
        LogEvent event = logParser.parse(raw);

        assertEquals("bob", event.getUsername());
        assertEquals("windows_login", event.getService());
    }

    /**
     * Coverage: classifyEvent (windows_event / ERROR severity).
     */
    @Test
    void testParse_WindowsError() {
        String raw = "server-03 Application Error occurred";
        LogEvent event = logParser.parse(raw);

        assertEquals("windows_event", event.getService());
        assertEquals("ERROR", event.getSeverity());
    }

    /**
     * Coverage: extractHostname (Safety fallback),
     * extractUsername (Unknown path), classifyEvent (else/unknown).
     */
    @Test
    void testParse_UnknownEverything() {
        // Single word message to trigger hostname "unknown"
        String raw = "nothing";
        LogEvent event = logParser.parse(raw);

        assertEquals("unknown", event.getHostname());
        assertEquals("unknown", event.getUsername());
        assertEquals("unknown", event.getService());
    }

    /**
     * Coverage: JSON exists but "message" key is missing.
     */
    @Test
    void testParse_JsonWithoutMessageKey() {
        String json = "{\"other_key\": \"val\"}";
        LogEvent event = logParser.parse(json);

        assertEquals(json, event.getRawMessage());
    }

    /**
     * Coverage: Blacklist check for "root".
     */
    @Test
    void testBlacklist_RootUser() {
        String raw = "host user root";
        LogEvent event = logParser.parse(raw);
        assertTrue(event.isBlacklisted());
    }
}