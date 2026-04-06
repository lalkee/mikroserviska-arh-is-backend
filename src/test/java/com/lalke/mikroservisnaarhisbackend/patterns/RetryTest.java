package com.lalke.mikroservisnaarhisbackend.patterns;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class RetryTest {

    private Retry retry;
    private static final int DEFAULT_MAX_ATTEMPTS = 3;

    @BeforeEach
    void setUp() {
        retry = new Retry(DEFAULT_MAX_ATTEMPTS, (millis) -> {});
    }

    @Test
    void execute_ShouldReturnResult_WhenSuccessfulOnFirstAttempt() throws Exception {
        Callable<String> action = () -> "Success";

        String result = retry.execute(action);

        assertEquals("Success", result);
    }

    @Test
    void execute_ShouldRetryAndSucceed_WhenExceptionThrownInitially() throws Exception {
        AtomicInteger attempts = new AtomicInteger(0);
        Callable<String> action = () -> {
            if (attempts.incrementAndGet() < 2) {
                throw new RuntimeException("Temporary failure");
            }
            return "Recovered";
        };

        String result = retry.execute(action);

        assertEquals("Recovered", result);
        assertEquals(2, attempts.get());
    }

    @Test
    void execute_ShouldThrowLastException_WhenMaxAttemptsReached() {
        Callable<String> action = () -> {
            throw new RuntimeException("Persistent failure");
        };

        RuntimeException exception = assertThrows(RuntimeException.class, () -> 
            retry.execute(action, 2)
        );

        assertEquals("Persistent failure", exception.getMessage());
    }

    @Test
    void execute_ShouldRespectCustomMaxAttempts() {
        AtomicInteger attempts = new AtomicInteger(0);
        int customMax = 5;
        Callable<String> action = () -> {
            attempts.incrementAndGet();
            throw new RuntimeException("Failure");
        };

        assertThrows(RuntimeException.class, () -> 
            retry.execute(action, customMax)
        );

        assertEquals(customMax, attempts.get());
    }

    @Test
    void execute_ShouldHandleInterruptedException() {
        Callable<String> action = () -> {
            throw new InterruptedException("Thread interrupted");
        };

        assertThrows(InterruptedException.class, () -> retry.execute(action));
        assertTrue(Thread.currentThread().isInterrupted());
    }
}