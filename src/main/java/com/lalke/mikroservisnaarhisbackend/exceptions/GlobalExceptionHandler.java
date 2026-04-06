package com.lalke.mikroservisnaarhisbackend.exceptions;

import com.lalke.mikroservisnaarhisbackend.patterns.CircuitBreaker.CircuitBreakerOpenException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.Map;
import java.util.concurrent.TimeoutException;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CircuitBreakerOpenException.class)
    public ResponseEntity<?> handleCircuitBreaker(CircuitBreakerOpenException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error", "Service Unavailable", "message", ex.getMessage() != null ? ex.getMessage() : "Circuit breaker is open"));
    }

    @ExceptionHandler(TimeoutException.class)
    public ResponseEntity<?> handleTimeout(TimeoutException ex) {
        return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT)
                .body(Map.of("error", "Timeout", "message", ex.getMessage() != null ? ex.getMessage() : "Request timed out"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGeneral(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Internal Server Error", "message", ex.getMessage() != null ? ex.getMessage() : "An unexpected error occurred"));
    }
}
