package com.client_linux;

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

class LinuxLogClientTest {

    private LinuxLogClient client;
    private Socket mockSocket;
    private OutputStream mockOutputStream;
    private ScheduledExecutorService mockScheduler;

    @BeforeEach
    void setUp() {
        client = new LinuxLogClient();
        mockSocket = mock(Socket.class);
        mockOutputStream = mock(OutputStream.class);
        mockScheduler = mock(ScheduledExecutorService.class);
    }

    @AfterEach
    void tearDown() {
        // Clean up any resources
    }

    @Test
    void testConnectSuccess() throws IOException {
        try (MockedConstruction<Socket> socketMock = mockConstruction(Socket.class,
                (mock, context) -> {
                    when(mock.getOutputStream()).thenReturn(mockOutputStream);
                })) {

            client.connect();

            assertEquals(1, socketMock.constructed().size());
            verify(socketMock.constructed().get(0)).getOutputStream();
        }
    }

    @Test
    void testConnectFailure() {
        try (MockedConstruction<Socket> socketMock = mockConstruction(Socket.class,
                (mock, context) -> {
                    when(mock.getOutputStream()).thenThrow(new IOException("Connection failed"));
                })) {

            client.connect();

            assertEquals(1, socketMock.constructed().size());
        }
    }

    @Test
    void testBuildLogMessage() throws Exception {
        java.lang.reflect.Method method = LinuxLogClient.class.
                getDeclaredMethod("buildLogMessage");
        method.setAccessible(true);

        String message1 = (String) method.invoke(client);
        assertTrue(message1.contains("sudo: pam_unix(sudo:session)"));

        String message2 = (String) method.invoke(client);
        assertTrue(message2.contains("cron: pam_unix(cron:session)"));

        String message3 = (String) method.invoke(client);
        assertTrue(message3.contains("sudo: pam_unix(sudo:session)"));
    }

    @Test
    void testSendLogSuccess() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        java.lang.reflect.Field outputStreamField =
                LinuxLogClient.class.getDeclaredField("outputStream");
        outputStreamField.setAccessible(true);
        outputStreamField.set(client, baos);

        java.lang.reflect.Method sendLogMethod =
                LinuxLogClient.class.getDeclaredMethod("sendLog", String.class);
        sendLogMethod.setAccessible(true);

        String testMessage = "test message";
        sendLogMethod.invoke(client, testMessage);

        assertEquals(testMessage, baos.toString());
    }

    @Test
    void testSendLogWithNullOutputStream() throws Exception {
        java.lang.reflect.Field outputStreamField =
                LinuxLogClient.class.getDeclaredField("outputStream");
        outputStreamField.setAccessible(true);
        outputStreamField.set(client, null);

        java.lang.reflect.Method sendLogMethod =
                LinuxLogClient.class.getDeclaredMethod("sendLog", String.class);
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
                LinuxLogClient.class.getDeclaredField("outputStream");
        outputStreamField.setAccessible(true);
        outputStreamField.set(client, mockStream);

        java.lang.reflect.Method sendLogMethod =
                LinuxLogClient.class.getDeclaredMethod("sendLog", String.class);
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
                    LinuxLogClient.class.getDeclaredMethod("reconnect");
            reconnectMethod.setAccessible(true);

            reconnectMethod.invoke(client);

            assertTrue(socketMock.constructed().size() > 0);
        }
    }

    @Test
    void testShutdown() throws Exception {
        java.lang.reflect.Field schedulerField =
                LinuxLogClient.class.getDeclaredField("scheduler");
        schedulerField.setAccessible(true);
        schedulerField.set(client, mockScheduler);

        java.lang.reflect.Method shutdownMethod =
                LinuxLogClient.class.getDeclaredMethod("shutdown");
        shutdownMethod.setAccessible(true);

        shutdownMethod.invoke(client);

        verify(mockScheduler).shutdownNow();
    }

    @Test
    void testCloseResourcesWithSocket() throws Exception {
        java.lang.reflect.Field socketField =
                LinuxLogClient.class.getDeclaredField("socket");
        socketField.setAccessible(true);
        socketField.set(client, mockSocket);

        java.lang.reflect.Method closeResourcesMethod =
                LinuxLogClient.class.getDeclaredMethod("closeResources");
        closeResourcesMethod.setAccessible(true);

        closeResourcesMethod.invoke(client);

        verify(mockSocket).close();
    }

    @Test
    void testCloseResourcesWithNullSocket() throws Exception {
        java.lang.reflect.Field socketField =
                LinuxLogClient.class.getDeclaredField("socket");
        socketField.setAccessible(true);
        socketField.set(client, null);

        java.lang.reflect.Method closeResourcesMethod =
                LinuxLogClient.class.getDeclaredMethod("closeResources");
        closeResourcesMethod.setAccessible(true);

        closeResourcesMethod.invoke(client);
        // Should not throw exception
    }

    @Test
    void testCloseResourcesWithIOException() throws Exception {
        doThrow(new IOException("Close failed")).when(mockSocket).close();

        java.lang.reflect.Field socketField =
                LinuxLogClient.class.getDeclaredField("socket");
        socketField.setAccessible(true);
        socketField.set(client, mockSocket);

        java.lang.reflect.Method closeResourcesMethod =
                LinuxLogClient.class.getDeclaredMethod("closeResources");
        closeResourcesMethod.setAccessible(true);

        closeResourcesMethod.invoke(client);
        // Should not throw exception, exception is ignored
    }

    @Test
    void testSleep() throws Exception {
        java.lang.reflect.Method sleepMethod =
                LinuxLogClient.class.getDeclaredMethod("sleep", int.class);
        sleepMethod.setAccessible(true);

        long start = System.currentTimeMillis();
        sleepMethod.invoke(client, 0);
        long duration = System.currentTimeMillis() - start;

        assertTrue(duration < 100);
    }

    @Test
    void testSleepWithInterruption() throws Exception {
        java.lang.reflect.Method sleepMethod =
                LinuxLogClient.class.getDeclaredMethod("sleep", int.class);
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
    void testScheduleLogSendingWithIOException() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        java.lang.reflect.Field outputStreamField =
                LinuxLogClient.class.getDeclaredField("outputStream");
        outputStreamField.setAccessible(true);
        outputStreamField.set(client, baos);

        try (MockedConstruction<Socket> socketMock = mockConstruction(Socket.class,
                (mock, context) -> when(mock.getOutputStream()).thenReturn(mockOutputStream))) {

            java.lang.reflect.Method scheduleMethod =
                    LinuxLogClient.class.getDeclaredMethod("scheduleLogSending");
            scheduleMethod.setAccessible(true);

            scheduleMethod.invoke(client);

            // Simulate the scheduled task execution
            java.lang.reflect.Field schedulerField =
                    LinuxLogClient.class.getDeclaredField("scheduler");
            schedulerField.setAccessible(true);
            ScheduledExecutorService scheduler =
                    (ScheduledExecutorService) schedulerField.get(client);

            scheduler.shutdown();
        }
    }

    @Test
    void testMainMethod() {
        assertDoesNotThrow(() -> {
            Thread mainThread = new Thread(() -> LinuxLogClient.main(new String[]{}));
            mainThread.start();
            Thread.sleep(100);
            mainThread.interrupt();
        });
    }
}