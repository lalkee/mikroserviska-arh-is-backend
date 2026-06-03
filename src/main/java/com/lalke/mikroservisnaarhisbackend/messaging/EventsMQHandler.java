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
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.lalke.mikroservisnaarhisbackend.exceptions.ErrorSimulator;
import com.lalke.mikroservisnaarhisbackend.patterns.CircuitBreaker;
import com.lalke.mikroservisnaarhisbackend.patterns.Timeout;

@Component
@RequiredArgsConstructor
public class EventsMQHandler {
    private final EventService eventService;
    private final EventRepository repository;
    private final RabbitTemplate rabbitTemplate;
    private final CircuitBreaker circuitBreaker;
    private final ErrorSimulator errorSimulator;
    private final Timeout timeout;

    @RabbitListener(queues = "event.get.all")
    public void handleGetAll(Map<String, Object> payload,
                             @Header(AmqpHeaders.REPLY_TO) String replyTo,
                             @Header(AmqpHeaders.CORRELATION_ID) String correlationId) {
        try {
            List<EventResponseDTO> response = circuitBreaker.execute(() -> {
                errorSimulator.throwTriggeredException();

                List<Event> events = repository.findAll();

                if (events.isEmpty()) {
                    return new ArrayList<EventResponseDTO>();
                }

                // collect ids to request speakers in one batch
                List<Long> ids = events.stream().map(Event::getId).collect(Collectors.toList());

                List<List<Speaker>> allSpeakers = rabbitTemplate.convertSendAndReceiveAsType(
                        "speaker.get.byEventIds",
                        ids,
                        new ParameterizedTypeReference<List<List<Speaker>>>() {}
                );

                List<EventResponseDTO> dtos = new ArrayList<>();
                for (int i = 0; i < events.size(); i++) {
                    List<Speaker> speakers = (allSpeakers != null && allSpeakers.size() > i) 
                                             ? allSpeakers.get(i) 
                                             : new ArrayList<>();
                    dtos.add(EventResponseDTO.from(events.get(i), speakers));
                }
                return dtos;
            });

            if (replyTo != null) {
                sendResponse(replyTo, correlationId, response);
            }
        } catch (Exception e) {
            System.err.println("Circuit Breaker or Logic Error: " + e.getMessage());
            if (replyTo != null) {
                sendResponse(replyTo, correlationId, Map.of("error", e.getMessage()));
            }
        }
    }

    @RabbitListener(queues = "event.get.id")
    public void handleGetById(Long id,
                              @Header(AmqpHeaders.REPLY_TO) String replyTo,
                              @Header(AmqpHeaders.CORRELATION_ID) String correlationId) {
        try {
            EventResponseDTO response = timeout.execute(() -> {
                errorSimulator.longOperation();

                Event event = repository.findById(id).orElse(null);

                if (event != null) {
                    List<List<Speaker>> result = rabbitTemplate.convertSendAndReceiveAsType(
                            "speaker.get.byEventIds",
                            Collections.singletonList(id),
                            new ParameterizedTypeReference<List<List<Speaker>>>() {}
                    );

                    List<Speaker> speakers = (result != null && !result.isEmpty())
                            ? result.get(0)
                            : new ArrayList<>();

                    return EventResponseDTO.from(event, speakers);
                }
                return null;
            });

            if (replyTo != null && response != null) {
                sendResponse(replyTo, correlationId, response);
            }
        } catch (Exception e) {
            System.err.println("[EventsMQHandler] GetById Error: " + e.getMessage());
            if (replyTo != null) {
                // Send standard error response for the frontend
                sendResponse(replyTo, correlationId, Map.of("error", "Operation timed out or failed: " + e.getMessage()));
            }
        }
    }

    @RabbitListener(queues = "event.save")
    public void handleSave(EventRequestDTO eventRequest,
                        @Header(AmqpHeaders.REPLY_TO) String replyTo,
                        @Header(AmqpHeaders.CORRELATION_ID) String correlationId) {
        
        eventService.save(eventRequest);

        // we wait until we get response from speakers service, so frontend ui doesnt update too early to stale data
        if (eventRequest.getSpeakerIds() != null && !eventRequest.getSpeakerIds().isEmpty()) {
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
        eventService.delete(id);
    }

    @RabbitListener(queues = "events.delete.speaker")
    public void handleDeleteSpeakerFromEvents(Map<String, Object> payload) {
        List<Integer> eventIds = (List<Integer>) payload.get("eventIds");
        Map<String, Object> speakerMap = (Map<String, Object>) payload.get("speaker");

        try {
            for (Integer eventId : eventIds) {
                Long eId = Long.valueOf(eventId);
                
                //only delete if this speaker was the only participant
                List<List<Speaker>> result = rabbitTemplate.convertSendAndReceiveAsType(
                        "speaker.get.byEventIds",
                        Collections.singletonList(eId),
                        new ParameterizedTypeReference<List<List<Speaker>>>() {}
                );

                if (result != null && !result.isEmpty() && result.get(0).isEmpty()) {
                    eventService.delete(eId);
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to clean up events. Requesting speaker restoration.");
            
            //send request to re-add the speaker and their participations
            Map<String, Object> restorePayload = new HashMap<>();
            restorePayload.put("speaker", speakerMap);
            restorePayload.put("eventIds", eventIds);
            
            rabbitTemplate.convertAndSend("speaker.restore", restorePayload);
        }
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