package com.lalke.mikroservisnaarhisbackend.config;

import com.lalke.mikroservisnaarhisbackend.patterns.Timeout;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class TimeoutConfig {

    @Bean
    public Timeout timeout() {
        return new Timeout(Duration.ofSeconds(2));
    }
}