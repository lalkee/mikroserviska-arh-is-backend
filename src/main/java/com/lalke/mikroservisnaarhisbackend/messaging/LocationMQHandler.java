package com.lalke.mikroservisnaarhisbackend.messaging;

import lombok.RequiredArgsConstructor;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Component;

import com.lalke.mikroservisnaarhisbackend.model.Location;
import com.lalke.mikroservisnaarhisbackend.repository.LocationRepository;

import java.util.List;

@Component
@RequiredArgsConstructor
public class LocationMQHandler {
    private final LocationRepository repository;

    @RabbitListener(queues = "location.get.all")
    @SendTo("location.get.all.res")
    public List<Location> handleGetAll(java.util.Map<String, Object> payload) {
        return repository.findAll();
    }

    @RabbitListener(queues = "location.get.id")
    @SendTo("location.get.id.res")
    public Location handleGetById(Long id) {
        return repository.findById(id.longValue()).orElse(null);
    }

    @RabbitListener(queues = "location.save")
    public void handleSave(Location location) {
        repository.save(location);
    }

    @RabbitListener(queues = "location.delete")
    public void handleDelete(Long id) {
        repository.deleteById(id);
    }
}