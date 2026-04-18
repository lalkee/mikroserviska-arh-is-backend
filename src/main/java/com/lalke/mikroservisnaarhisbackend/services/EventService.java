package com.lalke.mikroservisnaarhisbackend.services;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.amqp.core.Queue;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lalke.mikroservisnaarhisbackend.dto.EventRequestDTO;
import com.lalke.mikroservisnaarhisbackend.model.Event;
import com.lalke.mikroservisnaarhisbackend.model.OutboxRecord;
import com.lalke.mikroservisnaarhisbackend.repository.EventRepository;
import com.lalke.mikroservisnaarhisbackend.repository.LocationRepository;
import com.lalke.mikroservisnaarhisbackend.repository.OutboxRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventService {
    
    private final Queue participationSaveQueue;
    private final Queue participationDeleteQueue;
    private final EventRepository eventRepository;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;
    private final LocationRepository locationRepository;

    @Transactional
    public void save(EventRequestDTO request) {
        log.info("Starting save transaction for event: {}", request.getName());

        Event event = (request.getId() != null) 
        ? eventRepository.findById(request.getId()).orElse(new Event()) 
        : new Event();
        event.setName(request.getName());
        event.setAgenda(request.getAgenda());
        event.setDateTime(request.getDateTime());
        event.setDuration(request.getDuration());
        event.setRegistrationFee(request.getRegistrationFee());
        
        locationRepository.findById(request.getLocationId())
            .ifPresentOrElse(
                event::setLocation,
                () -> log.warn("Location ID {} not found. Event will have null location.", request.getLocationId())
            );

        Event savedEvent = eventRepository.save(event);
        log.info("Event saved with ID: {}", savedEvent.getId());

        log.debug("Processing {} speaker IDs for outbox.", request.getSpeakerIds().size());
        if (request.getSpeakerIds() != null && !request.getSpeakerIds().isEmpty()) {

            List<Map<String, Long>> participations = request.getSpeakerIds().stream()
                .map(speakerId -> {
                    Map<String, Long> map = new HashMap<>();
                    map.put("eventId", savedEvent.getId());
                    map.put("speakerId", speakerId);
                    return map;
                })
                .collect(Collectors.toList());

            OutboxRecord outboxRecord = new OutboxRecord();
            outboxRecord.setTimestamp(Instant.now());
            outboxRecord.setQueue(participationSaveQueue.getName());
            
            try {
                String jsonPayload = objectMapper.writeValueAsString(participations);
                log.debug("Generated JSON payload (length: {}): {}", jsonPayload.length(), jsonPayload);
                outboxRecord.setPayload(jsonPayload);
            } catch (Exception e) {
                log.error("JSON Serialization failed for Event {}", savedEvent.getId(), e);
                throw new RuntimeException("Error serializing Participations to JSON", e);
            }

            OutboxRecord savedRecord = outboxRepository.save(outboxRecord);
            log.info("OutboxRecord saved with ID: {} for queue: {}", savedRecord.getId(), savedRecord.getQueue());
        } else {
            log.warn("No speaker IDs provided. Skipping Outbox persistence.");
        }
    }

    @Transactional
    public void delete(Long id) {
        eventRepository.deleteById(id);

        OutboxRecord outboxRecord = new OutboxRecord();
        outboxRecord.setTimestamp(Instant.now());
        outboxRecord.setQueue(participationDeleteQueue.getName());
        outboxRecord.setPayload(id.toString());
        outboxRepository.save(outboxRecord);
    }
}