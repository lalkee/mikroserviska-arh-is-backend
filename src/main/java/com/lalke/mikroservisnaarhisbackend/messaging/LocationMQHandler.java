package com.lalke.mikroservisnaarhisbackend.messaging;

import java.util.List;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import com.lalke.mikroservisnaarhisbackend.model.Location;
import com.lalke.mikroservisnaarhisbackend.patterns.RetryDLQ;
import com.lalke.mikroservisnaarhisbackend.repository.LocationRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class LocationMQHandler {
    private final LocationRepository repository;
    private final RabbitTemplate rabbitTemplate;
    private final RetryDLQ retryDLQ;

    @RabbitListener(queues = "location.get.all")
    public void handleGetAll(java.util.Map<String, Object> payload,
                            @Header(AmqpHeaders.REPLY_TO) String replyTo,
                            @Header(AmqpHeaders.CORRELATION_ID) String correlationId) throws Exception {
        
        try {
            List<Location> locations = retryDLQ.executeWithDlq(payload, () -> {
                return repository.findAll();
            });
    
            if (replyTo != null) {
                sendResponse(replyTo, correlationId, locations);
            }
        } catch (Exception e) {
                System.err.println("Message moved to DLQ: " + e.getMessage());
            }
    }

    @RabbitListener(queues = "location.get.id")
    public void handleGetById(Long id,
                                @Header(AmqpHeaders.REPLY_TO) String replyTo,
                                @Header(AmqpHeaders.CORRELATION_ID) String correlationId) {
        
        Location location = repository.findById(id).orElse(null);
        
        if (replyTo != null) {
            sendResponse(replyTo, correlationId, location);
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