package com.log_collector.model;

import lombok.Data;

import java.time.Instant;
@Data
public class LogEvent {

    private Instant timestamp;
    private String service;
    private String eventCategory;
    private String severity;
    private String username;
    private String hostname;
    private String rawMessage;
    private boolean blacklisted;
}
