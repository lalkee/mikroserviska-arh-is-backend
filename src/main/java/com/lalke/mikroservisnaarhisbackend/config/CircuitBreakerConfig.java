package com.lalke.mikroservisnaarhisbackend.config;

import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.lalke.mikroservisnaarhisbackend.patterns.CircuitBreaker;

@Configuration
public class CircuitBreakerConfig {
    @Bean
    public CircuitBreaker circuitBreaker() {
        return new CircuitBreaker(3, 2, Duration.ofSeconds(30));
    }
}