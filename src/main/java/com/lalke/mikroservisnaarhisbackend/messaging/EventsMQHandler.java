package com.lalke.mikroservisnaarhisbackend.messaging;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Component;

import com.lalke.mikroservisnaarhisbackend.model.Event;
import com.lalke.mikroservisnaarhisbackend.repository.EventRepository;

import java.util.List;

@Component
@RequiredArgsConstructor
public class EventsMQHandler {
    private final EventRepository repository;

    @RabbitListener(queues = "event.get.all")
    @SendTo("event.get.all.res")
    public List<Event> handleGetAll(java.util.Map<String, Object> payload) {
        return repository.findAllWithDetails();
    }

    @RabbitListener(queues = "event.get.id")
    @SendTo("event.get.id.res")
    public Event handleGetById(Long id) {
        return repository.findByIdWithDetails(id).orElse(null);
    }

    @RabbitListener(queues = "event.save")
    public void handleSave(Event event) {
        repository.save(event);
    }

    @RabbitListener(queues = "event.delete")
    public void handleDelete(Long id) {
        repository.deleteById(id);
    }
}
