package com.lalke.mikroservisnaarhisbackend.patterns;

import java.time.Duration;
import java.util.concurrent.Callable;

public class Retry {
    private final int defaultMaxAttempts;
    /* needed to avoid tests lasting long. In tests, instead of Thread.sleep(),
    we will replace it with function that does nothing*/
    private final Sleeper sleeper;

    public Retry(int defaultMaxAttempts) {
        this.defaultMaxAttempts = defaultMaxAttempts;
        this.sleeper = Thread::sleep;
    }

    //only for testing
    public Retry(int defaultMaxAttempts, Sleeper sleeper) {
        this.defaultMaxAttempts = defaultMaxAttempts;
        this.sleeper = sleeper;
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
                        sleeper.sleep(Duration.ofSeconds(2 * i).toMillis());
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw ie;
                    }
                }
            }
        }
        throw lastException;
    }

    @FunctionalInterface
    public static interface Sleeper {
        void sleep(long millis) throws InterruptedException;
    }
}