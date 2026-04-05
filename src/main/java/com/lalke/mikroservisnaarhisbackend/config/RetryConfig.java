package com.lalke.mikroservisnaarhisbackend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.lalke.mikroservisnaarhisbackend.patterns.Retry;

@Configuration
public class RetryConfig {
    @Bean
    public Retry retry() {
        return new Retry(3);
    }
}
