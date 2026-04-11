package com.lalke.mikroservisnaarhisbackend.messaging;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import com.lalke.mikroservisnaarhisbackend.model.Event;
import com.lalke.mikroservisnaarhisbackend.repository.EventRepository;

import java.util.List;

@Component
@RequiredArgsConstructor
public class EventsMQHandler {
    private final EventRepository repository;
    private final RabbitTemplate rabbitTemplate;

    @RabbitListener(queues = "event.get.all")
    public void handleGetAll(java.util.Map<String, Object> payload,
                             @Header(AmqpHeaders.REPLY_TO) String replyTo,
                             @Header(AmqpHeaders.CORRELATION_ID) String correlationId) {
        
        List<Event> events = repository.findAllWithDetails();
        
        if (replyTo != null) {
            sendResponse(replyTo, correlationId, events);
        }
    }

    @RabbitListener(queues = "event.get.id")
    public void handleGetById(Long id,
                              @Header(AmqpHeaders.REPLY_TO) String replyTo,
                              @Header(AmqpHeaders.CORRELATION_ID) String correlationId) {
        
        Event event = repository.findByIdWithDetails(id).orElse(null);
        
        if (replyTo != null) {
            sendResponse(replyTo, correlationId, event);
        }
    }

    @RabbitListener(queues = "event.save")
    public void handleSave(Event event) {
        repository.save(event);
    }

    @RabbitListener(queues = "event.delete")
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