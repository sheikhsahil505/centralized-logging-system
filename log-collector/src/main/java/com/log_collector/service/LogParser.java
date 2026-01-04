package com.log_collector.service;

import com.log_collector.model.LogEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class LogParser {

    private static final Set<String> BLACKLIST =
            Set.of("root", "admin");

    private static final ObjectMapper mapper = new ObjectMapper();

    // Regex patterns
    private static final Pattern LINUX_USER_PATTERN =
            Pattern.compile("user\\s+(\\w+)");
    private static final Pattern WINDOWS_USER_PATTERN =
            Pattern.compile("Account Name:\\s*(\\w+)");

    public LogEvent parse(String raw) {

        String message = extractMessage(raw);

        LogEvent event = new LogEvent();
        event.setTimestamp(Instant.now());
        event.setRawMessage(message);

        event.setHostname(extractHostname(message));

        // Username
        String username = extractUsername(message);
        event.setUsername(username);

        //  Blacklist
        event.setBlacklisted(BLACKLIST.contains(username));

        // Service + category + severity
        classifyEvent(message, event);

        return event;
    }

    // Helpers


    private String extractMessage(String raw) {
        try {
            JsonNode node = mapper.readTree(raw);
            return node.has("message") ? node.get("message").asText() : raw;
        } catch (Exception e) {
            return raw; // fallback for safety
        }
    }

    private String extractUsername(String message) {

        Matcher linux = LINUX_USER_PATTERN.matcher(message);
        if (linux.find()) {
            return linux.group(1);
        }

        Matcher windows = WINDOWS_USER_PATTERN.matcher(message);
        if (windows.find()) {
            return windows.group(1);
        }

        return "unknown";
    }

    private String extractHostname(String message) {
        String[] parts = message.split(" ");
        return parts.length > 1 ? parts[1] : "unknown";
    }

    private void classifyEvent(String message, LogEvent event) {

        if (message.contains("sudo") || message.contains("session opened")) {
            event.setService("linux_login");
            event.setEventCategory("login.audit");
            event.setSeverity("INFO");

        } else if (message.contains("cron") || message.contains("session closed")) {
            event.setService("linux_logout");
            event.setEventCategory("logout.audit");
            event.setSeverity("INFO");

        } else if (message.contains("Microsoft-Windows-Security-Auditing")) {
            event.setService("windows_login");
            event.setEventCategory("login.audit");
            event.setSeverity("INFO");

        } else if (message.contains("Application Error")) {
            event.setService("windows_event");
            event.setEventCategory("system.event");
            event.setSeverity("ERROR");

        } else {
            event.setService("unknown");
            event.setEventCategory("unknown");
            event.setSeverity("INFO");
        }
    }
}
