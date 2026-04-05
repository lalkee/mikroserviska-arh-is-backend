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
        final State currentState = state;

        if (currentState == State.OPEN && 
            Duration.between(lastFailureTime, Instant.now()).compareTo(cooldown) >= 0)
            transitionToHalfOpen();
        
        if (currentState == State.OPEN)
            throw new CircuitBreakerOpenException("Circuit breaker is open");

        if (currentState == State.HALF_OPEN && halfOpenTrials.incrementAndGet() > halfOpenTrialLimit) {
            halfOpenTrials.decrementAndGet();
            throw new CircuitBreakerOpenException("Circuit breaker is half open");
        }

        try {
            T result = action.call();
            if (currentState == State.HALF_OPEN)
                transitionToClosed();
            failureCount.set(0);
            return result;
        } catch (Exception e) {
            if (currentState == State.CLOSED && failureCount.incrementAndGet() >= failureThreshold)
                transitionToOpen();
            else if (currentState == State.HALF_OPEN)
                transitionToOpen();
            throw e;
        }
    }

    private synchronized void transitionToOpen() {
        state = State.OPEN;
        failureCount.set(0);
        halfOpenTrials.set(0);
        lastFailureTime = Instant.now();
    }

    private synchronized void transitionToHalfOpen() {
        if (state == State.OPEN) {
            state = State.HALF_OPEN;
            halfOpenTrials.set(0);
        }
    }

    private synchronized void transitionToClosed() {
        if (state == State.HALF_OPEN) {
            state = State.CLOSED;
            failureCount.set(0);
            halfOpenTrials.set(0);
        }
    }

    public static class CircuitBreakerOpenException extends RuntimeException {
        public CircuitBreakerOpenException(String message) { super(message); }
    }
}