package com.lalke.mikroservisnaarhisbackend.patterns;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.Callable;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RetryTest {

    private Retry retry;
    private static final int DEFAULT_MAX_ATTEMPTS = 3;

    @Mock
    private Callable<String> action;

    @BeforeEach
    void setUp() {
        retry = new Retry(DEFAULT_MAX_ATTEMPTS, (millis) -> {});
    }

    @Test
    void execute_ShouldReturnResult_WhenSuccessfulOnFirstAttempt() throws Exception {
        when(action.call()).thenReturn("Success");

        String result = retry.execute(action);

        assertEquals("Success", result);
        verify(action, times(1)).call();
    }

    @Test
    void execute_ShouldRetryAndSucceed_WhenExceptionThrownInitially() throws Exception {
        when(action.call())
            .thenThrow(new RuntimeException("Temporary failure"))
            .thenReturn("Recovered");

        String result = retry.execute(action);

        assertEquals("Recovered", result);
        verify(action, times(2)).call();
    }

    @Test
    void execute_ShouldThrowLastException_WhenMaxAttemptsReached() throws Exception {
        when(action.call()).thenThrow(new RuntimeException("Persistent failure"));

        int maxAttempts = 2;
        RuntimeException exception = assertThrows(RuntimeException.class, () -> 
            retry.execute(action, maxAttempts)
        );

        assertEquals("Persistent failure", exception.getMessage());
        verify(action, times(maxAttempts)).call();
    }

    @Test
    void execute_ShouldRespectCustomMaxAttempts() throws Exception {
        when(action.call()).thenThrow(new RuntimeException("Failure"));

        int customMax = 5;
        assertThrows(RuntimeException.class, () -> 
            retry.execute(action, customMax)
        );

        verify(action, times(customMax)).call();
    }

    @Test
    void execute_ShouldHandleInterruptedException() throws Exception {
        when(action.call()).thenThrow(new InterruptedException("Thread interrupted"));

        assertThrows(InterruptedException.class, () -> retry.execute(action));
        assertTrue(Thread.currentThread().isInterrupted());
    }
}