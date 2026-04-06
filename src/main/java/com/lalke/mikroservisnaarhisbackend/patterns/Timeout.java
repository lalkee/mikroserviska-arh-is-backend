package com.lalke.mikroservisnaarhisbackend.patterns;

import java.util.concurrent.*;
import java.time.Duration;

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
            if (cause instanceof InterruptedException) {
                /*calling thread doesn't care worker was interupted,
                so we need to do it manually*/
                Thread.currentThread().interrupt();
                throw (InterruptedException) cause;
            }
            if (cause instanceof Exception) {
                throw (Exception) cause;
            }
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw e;
        }
    }
}