package com.lalke.mikroservisnaarhisbackend.patterns;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CircuitBreakerTest {

    private CircuitBreaker circuitBreaker;
    private static final int FAILURE_THRESHOLD = 2;
    private static final int TRIAL_LIMIT = 1;
    private static final Duration COOLDOWN = Duration.ofMillis(100);

    @Mock
    private Callable<String> action;

    @BeforeEach
    void setUp() {
        circuitBreaker = new CircuitBreaker(FAILURE_THRESHOLD, TRIAL_LIMIT, COOLDOWN);
    }

    @Test
    void execute_ShouldReturnResult_WhenInClosedState() throws Exception {
        when(action.call()).thenReturn("Success");

        String result = circuitBreaker.execute(action);

        assertEquals("Success", result);
        verify(action, times(1)).call();
    }

    @Test
    void execute_ShouldTransitionToOpen_WhenThresholdReached() throws Exception {
        when(action.call()).thenThrow(new RuntimeException("Fail"));

        assertThrows(RuntimeException.class, () -> circuitBreaker.execute(action));
        assertThrows(RuntimeException.class, () -> circuitBreaker.execute(action));
        
        assertThrows(CircuitBreaker.CircuitBreakerOpenException.class, () -> 
            circuitBreaker.execute(() -> "Won't run")
        );
        verify(action, times(2)).call();
    }

    @Test
    void execute_ShouldTransitionToHalfOpen_AfterCooldown() throws Exception {
        for (int i = 0; i < FAILURE_THRESHOLD; i++) {
            assertThrows(RuntimeException.class, () -> 
                circuitBreaker.execute(() -> { throw new RuntimeException(); })
            );
        }
        Thread.sleep(COOLDOWN.toMillis() + 10);
        String result = circuitBreaker.execute(() -> "Back in business");
        assertEquals("Back in business", result);
    }

    @Test
    void execute_ShouldEnforceTrialLimit_InHalfOpenState() throws Exception {
        for (int i = 0; i < FAILURE_THRESHOLD; i++) {
            assertThrows(RuntimeException.class, () -> 
                circuitBreaker.execute(() -> { throw new RuntimeException(); }));
        }
        
        Thread.sleep(COOLDOWN.toMillis() + 10);
        CountDownLatch latch = new CountDownLatch(1);
        
        Thread t1 = new Thread(() -> {
            try {
                circuitBreaker.execute(() -> {
                    latch.await();
                    return "First Trial Success";
                });
            } catch (Exception ignored) {}
        });
        t1.start();

        Thread.sleep(20);
        assertThrows(CircuitBreaker.CircuitBreakerOpenException.class, () -> 
            circuitBreaker.execute(() -> "Second Trial")
        );

        latch.countDown();
        t1.join();
    }

    @Test
    void execute_ShouldResetToClosed_AfterSuccessfulTrial() throws Exception {
        for (int i = 0; i < FAILURE_THRESHOLD; i++) {
            assertThrows(RuntimeException.class, () -> circuitBreaker.execute(() -> { throw new RuntimeException(); }));
        }
        Thread.sleep(COOLDOWN.toMillis() + 10);

        circuitBreaker.execute(() -> "Success");
        assertThrows(RuntimeException.class, () -> 
            circuitBreaker.execute(() -> { throw new RuntimeException("New fail"); })
        );
        assertEquals("Still Closed", circuitBreaker.execute(() -> "Still Closed"));
    }

    @Test
    void execute_ShouldReturnToOpen_IfTrialFails() throws Exception {
        for (int i = 0; i < FAILURE_THRESHOLD; i++) {
            assertThrows(RuntimeException.class, () -> circuitBreaker.execute(() -> { throw new RuntimeException(); }));
        }
        Thread.sleep(COOLDOWN.toMillis() + 10);

        when(action.call()).thenThrow(new RuntimeException("Trial failed"));

        assertThrows(RuntimeException.class, () -> circuitBreaker.execute(action));

        assertThrows(CircuitBreaker.CircuitBreakerOpenException.class, () -> 
            circuitBreaker.execute(() -> "Denied")
        );
    }

    @Test
    void execute_ShouldPropagateActualException_AndNotGenericRuntime() throws Exception {
        class DatabaseConnectionException extends RuntimeException {
            public DatabaseConnectionException(String message) { super(message); }
        }
        
        String specificMessage = "SQL State: 08001 - Unable to connect";
        when(action.call()).thenThrow(new DatabaseConnectionException(specificMessage));

        DatabaseConnectionException exception = assertThrows(
            DatabaseConnectionException.class, 
            () -> circuitBreaker.execute(action)
        );

        assertEquals(specificMessage, exception.getMessage());
    }
}