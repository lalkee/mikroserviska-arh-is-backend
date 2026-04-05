package com.lalke.mikroservisnaarhisbackend.patterns;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CompletionException;

public class Timeout {
    private final Duration defaultDuration;

    public Timeout(Duration defaultDuration) {
        this.defaultDuration = defaultDuration;
    }

    public <T> T execute(Callable<T> action) throws Exception {
        return execute(action, this.defaultDuration);
    }

    public <T> T execute(Callable<T> action, Duration duration) throws Exception {
        try {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    return action.call();
                } catch (Exception e) {
                    throw new CompletionException(e);
                }
            })
            .orTimeout(duration.toMillis(), TimeUnit.MILLISECONDS)
            .get();
        } catch (ExecutionException | CompletionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof Exception) {
                throw (Exception) cause;
            }
            throw e;
        }
    }
}