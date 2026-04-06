package com.lalke.mikroservisnaarhisbackend.patterns;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;

class TimeoutTest {

    private Timeout timeout;
    private static final Duration DEFAULT_DURATION = Duration.ofMillis(500);

    @BeforeEach
    void setUp() {
        timeout = new Timeout(DEFAULT_DURATION);
    }

    @Test
    void execute_ShouldReturnResult_WhenActionCompletesWithinTimeout() throws Exception {
        Callable<String> action = () -> "Success";

        String result = timeout.execute(action);

        assertEquals("Success", result);
    }

    @Test
    void execute_ShouldThrowTimeoutException_WhenActionExceedsDuration() {
        Callable<String> action = () -> {
            Thread.sleep(1000);
            return "Too late";
        };

        assertThrows(TimeoutException.class, () -> 
            timeout.execute(action, Duration.ofMillis(100))
        );
    }

    @Test
    void execute_ShouldUnwrapAndThrowOriginalException_WhenActionFails() {
        String errorMessage = "Original Database Error";
        Callable<String> action = () -> {
            throw new IllegalArgumentException(errorMessage);
        };

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> 
            timeout.execute(action)
        );

        assertEquals(errorMessage, exception.getMessage());
    }

    @Test
    void execute_ShouldRespectCustomDuration() throws Exception {
        Callable<String> action = () -> {
            Thread.sleep(300);
            return "Made it";
        };

        // Should succeed because 500ms > 300ms
        String result = timeout.execute(action, Duration.ofMillis(500));
        
        assertEquals("Made it", result);
    }

    @Test
    void execute_ShouldHandleInterruptedException() {
        Callable<String> action = () -> {
            throw new InterruptedException("Interrupted during task");
        };

        assertThrows(InterruptedException.class, () -> timeout.execute(action));
    }
}