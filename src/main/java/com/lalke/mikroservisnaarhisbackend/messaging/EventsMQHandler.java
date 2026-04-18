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

            // collect ids to request speakers in one batch
            List<Long> ids = events.stream().map(Event::getId).collect(Collectors.toList());

            List<List<Speaker>> allSpeakers = rabbitTemplate.convertSendAndReceiveAsType(
                    "speaker.get.byEventIds",
                    ids,
                    new ParameterizedTypeReference<List<List<Speaker>>>() {}
            );

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
            List<List<Speaker>> result = rabbitTemplate.convertSendAndReceiveAsType(
                    "speaker.get.byEventIds",
                    Collections.singletonList(id),
                    new ParameterizedTypeReference<List<List<Speaker>>>() {}
            );

            List<Speaker> speakers = (result != null && !result.isEmpty()) 
                                     ? result.get(0) 
                                     : new ArrayList<>();
            
            sendResponse(replyTo, correlationId, EventResponseDTO.from(event, speakers));
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

    private void sendResponse(String replyTo, String correlationId, Object payload) {
        rabbitTemplate.convertAndSend(replyTo, payload, message -> {
            if (correlationId != null) {
                message.getMessageProperties().setCorrelationId(correlationId);
            }
            return message;
        });
    }
}