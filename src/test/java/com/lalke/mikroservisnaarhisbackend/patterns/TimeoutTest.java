package com.lalke.mikroservisnaarhisbackend.patterns;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TimeoutTest {

    private Timeout timeout;
    private static final Duration DEFAULT_DURATION = Duration.ofMillis(250);

    @Mock
    private Callable<String> action;

    @BeforeEach
    void setUp() {
        timeout = new Timeout(DEFAULT_DURATION);
    }

    @Test
    void execute_ShouldReturnResult_WhenActionCompletesWithinTimeout() throws Exception {
        when(action.call()).thenReturn("Success");

        String result = timeout.execute(action);

        assertEquals("Success", result);
        verify(action, times(1)).call();
    }

    @Test
    void execute_ShouldThrowTimeoutException_WhenActionExceedsDuration() throws Exception {
        when(action.call()).thenAnswer(invocation -> {
            Thread.sleep(1000);
            return "Too late";
        });

        assertThrows(TimeoutException.class, () -> 
            timeout.execute(action, Duration.ofMillis(100))
        );
    }

    @Test
    void execute_ShouldUnwrapAndThrowOriginalException_WhenActionFails() throws Exception {
        String errorMessage = "Original Database Error";
        when(action.call()).thenThrow(new IllegalArgumentException(errorMessage));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> 
            timeout.execute(action)
        );

        assertEquals(errorMessage, exception.getMessage());
    }

    @Test
    void execute_ShouldRespectCustomDuration() throws Exception {
        when(action.call()).thenAnswer(invocation -> {
            Thread.sleep(50);
            return "Made it";
        });

        String result = timeout.execute(action, Duration.ofMillis(200));
        
        assertEquals("Made it", result);
        verify(action, times(1)).call();
    }

    @Test
    void execute_ShouldHandleInterruptedExceptionAndRestoreStatus() throws Exception {
        when(action.call()).thenThrow(new InterruptedException("Interrupted during task"));

        assertThrows(InterruptedException.class, () -> timeout.execute(action));
        
        assertTrue(Thread.currentThread().isInterrupted());
        
        Thread.interrupted();
    }
}