package com.lalke.mikroservisnaarhisbackend.patterns;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class Timeout {
    private final Duration defaultDuration;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public Timeout(Duration defaultDuration) {
        this.defaultDuration = defaultDuration;
    }

    public <T> T execute(Callable<T> action) throws Exception, TimeoutException {
        return execute(action, this.defaultDuration);
    }

    public <T> T execute(Callable<T> action, Duration duration) throws Exception, TimeoutException {
        Future<T> future = executor.submit(action);
        
        try {
            return future.get(duration.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw e;
        /*exceptions from worker thread (one running the action)
        are wrapped in ExecutionException*/
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof InterruptedException interruptedException) {
                /*calling thread doesn't care worker was interupted,
                so we need to do it manually*/
                Thread.currentThread().interrupt();
                throw interruptedException;
            }
            if (cause instanceof Exception exception) {
                throw exception;
            }
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw e;
        }
    }
}