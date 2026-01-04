package com.log_server.service;

import com.log_server.model.LogEvent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LogStoreTest {

    @Test
    void shouldStoreLogsInMemory() {
        LogStore store = new LogStore();

        store.add(new LogEvent());
        store.add(new LogEvent());

        assertEquals(2, store.getAll().size());
        assertEquals(2, store.size());

    }
}
