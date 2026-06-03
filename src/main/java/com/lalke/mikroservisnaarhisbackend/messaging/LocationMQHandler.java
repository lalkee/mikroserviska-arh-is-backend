package com.lalke.mikroservisnaarhisbackend.messaging;

import java.util.List;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import com.lalke.mikroservisnaarhisbackend.exceptions.ErrorSimulator;
import com.lalke.mikroservisnaarhisbackend.model.Location;
import com.lalke.mikroservisnaarhisbackend.patterns.Retry;
import com.lalke.mikroservisnaarhisbackend.patterns.RetryDLQ;
import com.lalke.mikroservisnaarhisbackend.repository.LocationRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class LocationMQHandler {
    private final LocationRepository repository;
    private final RabbitTemplate rabbitTemplate;
    private final RetryDLQ retryDLQ;
    private final Retry retry;
    private final ErrorSimulator errorSimulator;

    @RabbitListener(queues = "location.get.all")
    public void handleGetAll(java.util.Map<String, Object> payload,
                            @Header(AmqpHeaders.REPLY_TO) String replyTo,
                            @Header(AmqpHeaders.CORRELATION_ID) String correlationId) {
        try {
            List<Location> locations = retryDLQ.executeWithDlq(payload, "location.get.all", () -> {
                errorSimulator.throwTriggeredException(); 
                return repository.findAll();
            });

            if (replyTo != null) {
                sendResponse(replyTo, correlationId, locations);
            }
        } catch (Exception e) {
            if (replyTo != null) {
                java.util.Map<String, String> errorResponse = java.util.Map.of("error", "Failed to fetch locations");
                sendResponse(replyTo, correlationId, errorResponse);
            }
        }
    }

    @RabbitListener(queues = "location.get.id")
    public void handleGetById(Long id,
                            @Header(AmqpHeaders.REPLY_TO) String replyTo,
                            @Header(AmqpHeaders.CORRELATION_ID) String correlationId) {
        try {
            Location location = retry.execute(() -> {
                errorSimulator.throwTriggeredException(); 
                return repository.findById(id).orElse(null);
            });

            if (replyTo != null) {
                sendResponse(replyTo, correlationId, location);
            }
        } catch (Exception e) {
            System.err.println("Error fetching location: " + e.getMessage());
            if (replyTo != null) {
                java.util.Map<String, String> errorResponse = java.util.Map.of(
                    "error", "Could not find location with ID: " + id
                );
                sendResponse(replyTo, correlationId, errorResponse);
            }
        }
    }

    @RabbitListener(queues = "location.save")
    public void handleSave(Location location) {
        repository.save(location);
    }

    @RabbitListener(queues = "location.delete")
    public void handleDelete(Long id) {
        repository.deleteById(id);
    }

    private void sendResponse(String replyTo, String correlationId, Object payload) {
        rabbitTemplate.convertAndSend(replyTo, payload, message -> {
            if (correlationId != null) {
                message.getMessageProperties().setCorrelationId(correlationId);
            }
            return message;
        });
    }
}