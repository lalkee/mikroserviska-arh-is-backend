package com.lalke.mikroservisnaarhisbackend.patterns;

import java.time.Duration;
import java.util.concurrent.Callable;

public class Retry {
    private final int defaultMaxAttempts;

    public Retry(int defaultMaxAttempts) {
        this.defaultMaxAttempts = defaultMaxAttempts;
    }

    public <T> T execute(Callable<T> action) throws Exception {
        return execute(action, this.defaultMaxAttempts);
    }

    public <T> T execute(Callable<T> action, int maxAttempts) throws Exception {
        Exception lastException = new RuntimeException("Undefined exception");

        for (int i = 1; i <= maxAttempts; i++) {
            try {
                return action.call();
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw ie;
            } catch (Exception e) {
                lastException = e;
                if (i < maxAttempts) {
                    try {
                        Thread.sleep(Duration.ofSeconds(2 * i));
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw ie;
                    }
                }
            }
        }
        throw lastException;
    }
}