package com.lalke.mikroservisnaarhisbackend.controller;

import com.lalke.mikroservisnaarhisbackend.model.Event;
import com.lalke.mikroservisnaarhisbackend.patterns.CircuitBreaker;
import com.lalke.mikroservisnaarhisbackend.patterns.Retry;
import com.lalke.mikroservisnaarhisbackend.patterns.Timeout;
import com.lalke.mikroservisnaarhisbackend.repository.EventRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EventController.class)
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
class EventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EventRepository eventRepository;

    @TestConfiguration
    static class ResilienceTestConfig {
        @Bean
        public Retry retry() {
            return new Retry(3, millis -> {}); 
        }

        @Bean
        public Timeout timeout() {
            return new Timeout(Duration.ofMillis(100));
        }

        @Bean
        public CircuitBreaker circuitBreaker() {
            return new CircuitBreaker(2, 1, Duration.ofMinutes(1));
        }
    }

    @Test
    @DisplayName("Interaction: Happy Path through MVC stack")
    void testHappyPath() throws Exception {
        when(eventRepository.findAll()).thenReturn(List.of(new Event()));

        mockMvc.perform(get("/api/events"))
                .andExpect(status().isOk());

        verify(eventRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Interaction: Timeout layer triggers Retry, which succeeds")
    void testTimeoutTriggersRetryInteraction() throws Exception {
        when(eventRepository.findAll())
            .thenAnswer(inv -> { Thread.sleep(200); return null; })
            .thenReturn(List.of(new Event()));

        mockMvc.perform(get("/api/events"))
                .andExpect(status().isOk());

        verify(eventRepository, times(2)).findAll();
    }

    @Test
    @DisplayName("Interaction: Repeated Timeouts escalate to open the Circuit Breaker")
    void testTimeoutEscalationToCircuitBreaker() throws Exception {
        when(eventRepository.findAll()).thenAnswer(inv -> {
            Thread.sleep(200);
            return null;
        });

        // Request 1: 3 attempts. Fail with 504. CB count = 1.
        mockMvc.perform(get("/api/events")).andExpect(status().isGatewayTimeout());

        // Request 2: 3 attempts. Fail with 504. CB count = 2. State -> OPEN.
        mockMvc.perform(get("/api/events")).andExpect(status().isGatewayTimeout());

        // Request 3: Circuit is OPEN. Fails immediately with 503.
        mockMvc.perform(get("/api/events"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(result -> {
                    Exception ex = result.getResolvedException();
                    assertTrue(ex instanceof CircuitBreaker.CircuitBreakerOpenException);
                });

        verify(eventRepository, times(6)).findAll();
    }

    @Test
    @DisplayName("Interaction: Immediate Exception propagation to Circuit Breaker")
    void testExceptionEscalationInteraction() throws Exception {
        when(eventRepository.findAll()).thenThrow(new RuntimeException("DB Down"));

        // Request 1: 3 attempts. Fail with 500. CB count = 1.
        mockMvc.perform(get("/api/events")).andExpect(status().isInternalServerError());
        
        // Request 2: 3 attempts. Fail with 500. CB count = 2. State -> OPEN.
        mockMvc.perform(get("/api/events")).andExpect(status().isInternalServerError());

        // Request 3: Blocked by Breaker (503)
        mockMvc.perform(get("/api/events"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(result -> assertTrue(result.getResolvedException() instanceof CircuitBreaker.CircuitBreakerOpenException));

        verify(eventRepository, times(6)).findAll();
    }

    @Test
    @DisplayName("Interaction: InterruptedException bypasses Retry loop")
    void testInterruptionInteraction() throws Exception {
        when(eventRepository.findAll()).thenAnswer(inv -> { throw new InterruptedException("Kill Thread"); });

        mockMvc.perform(get("/api/events"))
                .andExpect(status().isInternalServerError());

        verify(eventRepository, times(1)).findAll();
    }
}
