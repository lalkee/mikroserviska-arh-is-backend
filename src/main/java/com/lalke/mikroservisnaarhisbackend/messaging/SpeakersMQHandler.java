package com.lalke.mikroservisnaarhisbackend.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import com.lalke.mikroservisnaarhisbackend.model.Speaker;
import com.lalke.mikroservisnaarhisbackend.repository.SpeakerRepository;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SpeakersMQHandler {
    private final SpeakerRepository repository;
    private final RabbitTemplate rabbitTemplate;

    @RabbitListener(queues = "speaker.get.all")
    public void handleGetAll(java.util.Map<String, Object> payload,
                            @Header(AmqpHeaders.REPLY_TO) String replyTo,
                            @Header(AmqpHeaders.CORRELATION_ID) String correlationId) {
        
        List<Speaker> speakers = repository.findAll();
        
        if (replyTo != null) {
            sendResponse(replyTo, correlationId, speakers);
        }
    }

    @RabbitListener(queues = "speaker.get.id")
    public void handleGetById(Long id, 
                             @Header(AmqpHeaders.REPLY_TO) String replyTo,
                             @Header(AmqpHeaders.CORRELATION_ID) String correlationId) {
        
        log.info("Processing request for ID: {} | ReplyTo: {}", id, replyTo);
        
        Speaker speaker = repository.findById(id).orElse(null);

        if (replyTo != null) {
            sendResponse(replyTo, correlationId, speaker);
            log.info("Response sent to {}", replyTo);
        }
    }

    @RabbitListener(queues = "speaker.save")
    public void handleSave(Speaker speaker) {
        repository.save(speaker);
    }

    @RabbitListener(queues = "speaker.delete")
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