package com.log_server.service;

import com.log_server.model.LogEvent;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

@Service
public class LogStore {

    private final Queue<LogEvent> logs = new ConcurrentLinkedQueue<>();

    public void add(LogEvent log) {
        logs.add(log);
    }

    public List<LogEvent> getAll() {
        return logs.stream().collect(Collectors.toList());
    }

    public int size() {
        return logs.size();
    }
}
