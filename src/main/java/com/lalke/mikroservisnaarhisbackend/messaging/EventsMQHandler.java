package com.lalke.mikroservisnaarhisbackend.messaging;

import com.lalke.mikroservisnaarhisbackend.dto.EventRequestDTO;
import com.lalke.mikroservisnaarhisbackend.dto.EventResponseDTO;
import com.lalke.mikroservisnaarhisbackend.model.Event;
import com.lalke.mikroservisnaarhisbackend.model.Speaker;
import com.lalke.mikroservisnaarhisbackend.repository.EventRepository;
import com.lalke.mikroservisnaarhisbackend.services.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class EventsMQHandler {
    private final EventService eventService;
    private final EventRepository repository;
    private final RabbitTemplate rabbitTemplate;

    @RabbitListener(queues = "event.get.all")
    public void handleGetAll(java.util.Map<String, Object> payload,
                             @Header(AmqpHeaders.REPLY_TO) String replyTo,
                             @Header(AmqpHeaders.CORRELATION_ID) String correlationId) {
        
        List<Event> events = repository.findAll();
        
        if (replyTo != null) {
            if (events.isEmpty()) {
                sendResponse(replyTo, correlationId, new ArrayList<>());
                return;
            }

            // 1. Collect IDs to request speakers in one batch
            List<Long> ids = events.stream().map(Event::getId).collect(Collectors.toList());

            // 2. RPC call to Clojure Speaker service
            // Expecting a List of Lists of Speakers matching the order of 'ids'
            List<List<Speaker>> allSpeakers = (List<List<Speaker>>) rabbitTemplate.convertSendAndReceive(
                    "speaker.get.byEventIds", 
                    ids
            );

            // 3. Assemble DTOs
            List<EventResponseDTO> response = new ArrayList<>();
            for (int i = 0; i < events.size(); i++) {
                List<Speaker> speakers = (allSpeakers != null && allSpeakers.size() > i) 
                                         ? allSpeakers.get(i) 
                                         : new ArrayList<>();
                response.add(EventResponseDTO.from(events.get(i), speakers));
            }

            sendResponse(replyTo, correlationId, response);
        }
    }

    @RabbitListener(queues = "event.get.id")
    public void handleGetById(Long id,
                              @Header(AmqpHeaders.REPLY_TO) String replyTo,
                              @Header(AmqpHeaders.CORRELATION_ID) String correlationId) {
        
        Event event = repository.findById(id).orElse(null);
        
        if (event != null && replyTo != null) {
            // Use the same batch endpoint for a single ID to keep Clojure service simple
            List<List<Speaker>> result = (List<List<Speaker>>) rabbitTemplate.convertSendAndReceive(
                    "speaker.get.byEventIds", 
                    Collections.singletonList(id)
            );

            List<Speaker> speakers = (result != null && !result.isEmpty()) 
                                     ? result.get(0) 
                                     : new ArrayList<>();
            
            sendResponse(replyTo, correlationId, EventResponseDTO.from(event, speakers));
        }
    }

    // In EventsMQHandler.java
    @RabbitListener(queues = "event.save")
    public void handleSave(EventRequestDTO eventRequest,
                        @Header(AmqpHeaders.REPLY_TO) String replyTo,
                        @Header(AmqpHeaders.CORRELATION_ID) String correlationId) {
        
        eventService.save(eventRequest);

        // If there are speakers, we MUST wait for Clojure to finish before telling React to refresh
        if (eventRequest.getSpeakerIds() != null && !eventRequest.getSpeakerIds().isEmpty()) {
            // This acts as a synchronous block waiting for the "Done" signal from Clojure
            rabbitTemplate.receiveAndConvert("participation.save.res", 5000); 
        }

        if (replyTo != null) {
            Map<String, String> response = new HashMap<>();
            response.put("status", "SAVED");
            sendResponse(replyTo, correlationId, response);
        }
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