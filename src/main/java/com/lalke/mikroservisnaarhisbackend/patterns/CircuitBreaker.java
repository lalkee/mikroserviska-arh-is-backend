package com.lalke.mikroservisnaarhisbackend.patterns;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CircuitBreaker {
    enum State {CLOSED, OPEN, HALF_OPEN}

    private final int failureThreshold;
    private final int halfOpenTrialLimit;
    private final Duration cooldown;
    private final AtomicInteger failureCount = new AtomicInteger(0);
    private final AtomicInteger halfOpenTrials = new AtomicInteger(0);
    private volatile State state = State.CLOSED;
    private volatile Instant lastFailureTime = Instant.MIN;

    public <T> T execute(Callable<T> action) throws Exception {
        if (state == State.OPEN && 
            Duration.between(lastFailureTime, Instant.now()).compareTo(cooldown) >= 0) {
            transitionToHalfOpen();
        }

        final State currentState = state; 

        if (currentState == State.OPEN) {
            System.out.println("[CircuitBreaker] Execution blocked: State is OPEN");
            throw new CircuitBreakerOpenException("Circuit breaker is open");
        }

        if (currentState == State.HALF_OPEN) {
            int trial = halfOpenTrials.incrementAndGet();
            System.out.println("[CircuitBreaker] Trial " + trial + " in HALF_OPEN state");
            if (trial > halfOpenTrialLimit) {
                halfOpenTrials.decrementAndGet();
                throw new CircuitBreakerOpenException("Circuit breaker is open (Half-Open limit reached)");
            }
        }

        try {
            T result = action.call();
            if (state == State.HALF_OPEN) {
                System.out.println("[CircuitBreaker] Success in HALF_OPEN! Closing circuit.");
                transitionToClosed();
            }
            failureCount.set(0);
            return result;
        } catch (Exception e) {
            int currentFailures = failureCount.incrementAndGet();
            System.err.println("[CircuitBreaker] Action failed: " + e.getMessage() + " | Failure count: " + currentFailures);
            
            if (state == State.HALF_OPEN || (state == State.CLOSED && currentFailures >= failureThreshold)) {
                transitionToOpen();
            }
            throw e;
        }
    }

    private synchronized void transitionToOpen() {
        System.out.println("[CircuitBreaker] Changing to OPEN state at " + Instant.now());
        state = State.OPEN;
        failureCount.set(0);
        halfOpenTrials.set(0);
        lastFailureTime = Instant.now();
    }

    private synchronized void transitionToHalfOpen() {
        System.out.println("[CircuitBreaker] Cooldown over. Transitioning to HALF_OPEN");
        state = State.HALF_OPEN;
        halfOpenTrials.set(0);
    }

    private synchronized void transitionToClosed() {
        System.out.println("[CircuitBreaker] SUCCESS. Transitioning to CLOSED");
        state = State.CLOSED;
        failureCount.set(0);
        halfOpenTrials.set(0);
    }

    public static class CircuitBreakerOpenException extends RuntimeException {
        public CircuitBreakerOpenException(String message) { super(message); }
    }
}