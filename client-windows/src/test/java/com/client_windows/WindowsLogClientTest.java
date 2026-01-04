package com.client_windows;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.ScheduledExecutorService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class WindowsLogClientTest {

    private WindowsLogClient client;
    private Socket mockSocket;
    private OutputStream mockOutputStream;
    private ScheduledExecutorService mockScheduler;

    @BeforeEach
    void setUp() {
        client = new WindowsLogClient();
        mockSocket = mock(Socket.class);
        mockOutputStream = mock(OutputStream.class);
        mockScheduler = mock(ScheduledExecutorService.class);
    }

    @AfterEach
    void tearDown() {
        // Clean up any resources
    }

    @Test
    void testConnectSuccess() throws Exception {
        try (MockedConstruction<Socket> socketMock = mockConstruction(Socket.class,
                (mock, context) -> {
                    when(mock.getOutputStream()).thenReturn(mockOutputStream);
                })) {

            java.lang.reflect.Method connectMethod =
                    WindowsLogClient.class.getDeclaredMethod("connect");
            connectMethod.setAccessible(true);
            connectMethod.invoke(client);

            assertEquals(1, socketMock.constructed().size());
            verify(socketMock.constructed().get(0)).getOutputStream();
        }
    }

    @Test
    void testConnectFailure() throws Exception {
        try (MockedConstruction<Socket> socketMock = mockConstruction(Socket.class,
                (mock, context) -> {
                    when(mock.getOutputStream()).thenThrow(new IOException("Connection failed"));
                })) {

            java.lang.reflect.Method connectMethod =
                    WindowsLogClient.class.getDeclaredMethod("connect");
            connectMethod.setAccessible(true);
            connectMethod.invoke(client);

            assertEquals(1, socketMock.constructed().size());
        }
    }

    @Test
    void testBuildLogMessage() throws Exception {
        java.lang.reflect.Method method = WindowsLogClient.class.
                getDeclaredMethod("buildLogMessage");
        method.setAccessible(true);

        String message1 = (String) method.invoke(client);
        assertTrue(message1.contains("Microsoft-Windows-Security-Auditing"));
        assertTrue(message1.contains("Account Name: admin"));

        String message2 = (String) method.invoke(client);
        assertTrue(message2.contains("Application Error"));
        assertTrue(message2.contains("Application crash detected"));

        String message3 = (String) method.invoke(client);
        assertTrue(message3.contains("Microsoft-Windows-Security-Auditing"));
    }

    @Test
    void testSendLogSuccess() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        java.lang.reflect.Field outputStreamField =
                WindowsLogClient.class.getDeclaredField("outputStream");
        outputStreamField.setAccessible(true);
        outputStreamField.set(client, baos);

        java.lang.reflect.Method sendLogMethod =
                WindowsLogClient.class.getDeclaredMethod("sendLog", String.class);
        sendLogMethod.setAccessible(true);

        String testMessage = "test message";
        sendLogMethod.invoke(client, testMessage);

        assertEquals(testMessage, baos.toString());
    }

    @Test
    void testSendLogWithNullOutputStream() throws Exception {
        java.lang.reflect.Field outputStreamField =
                WindowsLogClient.class.getDeclaredField("outputStream");
        outputStreamField.setAccessible(true);
        outputStreamField.set(client, null);

        java.lang.reflect.Method sendLogMethod =
                WindowsLogClient.class.getDeclaredMethod("sendLog", String.class);
        sendLogMethod.setAccessible(true);

        assertThrows(java.lang.reflect.InvocationTargetException.class, () -> {
            sendLogMethod.invoke(client, "test");
        });
    }

    @Test
    void testSendLogIOException() throws Exception {
        OutputStream mockStream = mock(OutputStream.class);
        doThrow(new IOException("Write failed")).when(mockStream).write(any(byte[].class));

        java.lang.reflect.Field outputStreamField =
                WindowsLogClient.class.getDeclaredField("outputStream");
        outputStreamField.setAccessible(true);
        outputStreamField.set(client, mockStream);

        java.lang.reflect.Method sendLogMethod =
                WindowsLogClient.class.getDeclaredMethod("sendLog", String.class);
        sendLogMethod.setAccessible(true);

        assertThrows(java.lang.reflect.InvocationTargetException.class, () -> {
            sendLogMethod.invoke(client, "test");
        });
    }

    @Test
    void testReconnect() throws Exception {
        try (MockedConstruction<Socket> socketMock = mockConstruction(Socket.class,
                (mock, context) -> when(mock.getOutputStream()).thenReturn(mockOutputStream))) {

            java.lang.reflect.Method reconnectMethod =
                    WindowsLogClient.class.getDeclaredMethod("reconnect");
            reconnectMethod.setAccessible(true);

            reconnectMethod.invoke(client);

            assertTrue(socketMock.constructed().size() > 0);
        }
    }

    @Test
    void testShutdown() throws Exception {
        java.lang.reflect.Field schedulerField =
                WindowsLogClient.class.getDeclaredField("scheduler");
        schedulerField.setAccessible(true);
        schedulerField.set(client, mockScheduler);

        java.lang.reflect.Method shutdownMethod =
                WindowsLogClient.class.getDeclaredMethod("shutdown");
        shutdownMethod.setAccessible(true);

        shutdownMethod.invoke(client);

        verify(mockScheduler).shutdownNow();
    }

    @Test
    void testCloseResourcesWithSocket() throws Exception {
        java.lang.reflect.Field socketField =
                WindowsLogClient.class.getDeclaredField("socket");
        socketField.setAccessible(true);
        socketField.set(client, mockSocket);

        java.lang.reflect.Method closeResourcesMethod =
                WindowsLogClient.class.getDeclaredMethod("closeResources");
        closeResourcesMethod.setAccessible(true);

        closeResourcesMethod.invoke(client);

        verify(mockSocket).close();
    }

    @Test
    void testCloseResourcesWithNullSocket() throws Exception {
        java.lang.reflect.Field socketField =
                WindowsLogClient.class.getDeclaredField("socket");
        socketField.setAccessible(true);
        socketField.set(client, null);

        java.lang.reflect.Method closeResourcesMethod =
                WindowsLogClient.class.getDeclaredMethod("closeResources");
        closeResourcesMethod.setAccessible(true);

        closeResourcesMethod.invoke(client);
        // Should not throw exception
    }

    @Test
    void testCloseResourcesWithIOException() throws Exception {
        doThrow(new IOException("Close failed")).when(mockSocket).close();

        java.lang.reflect.Field socketField =
                WindowsLogClient.class.getDeclaredField("socket");
        socketField.setAccessible(true);
        socketField.set(client, mockSocket);

        java.lang.reflect.Method closeResourcesMethod =
                WindowsLogClient.class.getDeclaredMethod("closeResources");
        closeResourcesMethod.setAccessible(true);

        closeResourcesMethod.invoke(client);
        // Should not throw exception, exception is ignored
    }

    @Test
    void testSleep() throws Exception {
        java.lang.reflect.Method sleepMethod =
                WindowsLogClient.class.getDeclaredMethod("sleep", int.class);
        sleepMethod.setAccessible(true);

        long start = System.currentTimeMillis();
        sleepMethod.invoke(client, 0);
        long duration = System.currentTimeMillis() - start;

        assertTrue(duration < 100);
    }

    @Test
    void testSleepWithInterruption() throws Exception {
        java.lang.reflect.Method sleepMethod =
                WindowsLogClient.class.getDeclaredMethod("sleep", int.class);
        sleepMethod.setAccessible(true);

        Thread testThread = new Thread(() -> {
            try {
                sleepMethod.invoke(client, 5);
            } catch (Exception e) {
                // Expected
            }
        });

        testThread.start();
        Thread.sleep(50);
        testThread.interrupt();
        testThread.join(1000);

        assertTrue(testThread.isInterrupted());
    }

    @Test
    void testScheduleLogSending() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        java.lang.reflect.Field outputStreamField =
                WindowsLogClient.class.getDeclaredField("outputStream");
        outputStreamField.setAccessible(true);
        outputStreamField.set(client, baos);

        try (MockedConstruction<Socket> socketMock = mockConstruction(Socket.class,
                (mock, context) -> when(mock.getOutputStream()).thenReturn(mockOutputStream))) {

            java.lang.reflect.Method scheduleMethod =
                    WindowsLogClient.class.getDeclaredMethod("scheduleLogSending");
            scheduleMethod.setAccessible(true);

            scheduleMethod.invoke(client);

            // Simulate the scheduled task execution
            java.lang.reflect.Field schedulerField =
                    WindowsLogClient.class.getDeclaredField("scheduler");
            schedulerField.setAccessible(true);
            ScheduledExecutorService scheduler =
                    (ScheduledExecutorService) schedulerField.get(client);

            scheduler.shutdown();
        }
    }

    @Test
    void testMainMethod() {
        assertDoesNotThrow(() -> {
            Thread mainThread = new Thread(() -> WindowsLogClient.main(new String[]{}));
            mainThread.start();
            Thread.sleep(100);
            mainThread.interrupt();
        });
    }

    @Test
    void testScheduleLogSendingWithReconnect() throws Exception {
        // Set up null output stream to trigger IOException
        java.lang.reflect.Field outputStreamField =
                WindowsLogClient.class.getDeclaredField("outputStream");
        outputStreamField.setAccessible(true);
        outputStreamField.set(client, null);

        try (MockedConstruction<Socket> socketMock = mockConstruction(Socket.class,
                (mock, context) -> when(mock.getOutputStream()).thenReturn(mockOutputStream))) {

            java.lang.reflect.Method scheduleMethod =
                    WindowsLogClient.class.getDeclaredMethod("scheduleLogSending");
            scheduleMethod.setAccessible(true);

            scheduleMethod.invoke(client);

            // Give time for the scheduled task to attempt execution
            Thread.sleep(100);

            java.lang.reflect.Field schedulerField =
                    WindowsLogClient.class.getDeclaredField("scheduler");
            schedulerField.setAccessible(true);
            ScheduledExecutorService scheduler =
                    (ScheduledExecutorService) schedulerField.get(client);

            scheduler.shutdownNow();
        }
    }

    @Test
    void testAtomicIndexIncrement() throws Exception {
        java.lang.reflect.Method buildLogMethod =
                WindowsLogClient.class.getDeclaredMethod("buildLogMessage");
        buildLogMethod.setAccessible(true);

        // Call multiple times to test index wrapping
        for (int i = 0; i < 5; i++) {
            String message = (String) buildLogMethod.invoke(client);
            assertNotNull(message);
            assertTrue(message.startsWith("{\"message\":\""));
            assertTrue(message.endsWith("\"}\n"));
        }
    }

    @Test
    void testSocketFieldAccess() throws Exception {
        java.lang.reflect.Field socketField =
                WindowsLogClient.class.getDeclaredField("socket");
        socketField.setAccessible(true);

        assertNull(socketField.get(client));

        socketField.set(client, mockSocket);
        assertEquals(mockSocket, socketField.get(client));
    }

    @Test
    void testOutputStreamFlush() throws Exception {
        OutputStream mockStream = mock(OutputStream.class);

        java.lang.reflect.Field outputStreamField =
                WindowsLogClient.class.getDeclaredField("outputStream");
        outputStreamField.setAccessible(true);
        outputStreamField.set(client, mockStream);

        java.lang.reflect.Method sendLogMethod =
                WindowsLogClient.class.getDeclaredMethod("sendLog", String.class);
        sendLogMethod.setAccessible(true);

        sendLogMethod.invoke(client, "test");

        verify(mockStream).write(any(byte[].class));
        verify(mockStream).flush();
    }

    @Test
    void testLogMessageFormat() throws Exception {
        java.lang.reflect.Method buildLogMethod =
                WindowsLogClient.class.getDeclaredMethod("buildLogMessage");
        buildLogMethod.setAccessible(true);

        String message = (String) buildLogMethod.invoke(client);

        assertTrue(message.matches("\\{\"message\":\".*\"\\}\n"));
    }
}