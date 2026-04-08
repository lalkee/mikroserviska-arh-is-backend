package com.lalke.mikroservisnaarhisbackend.messaging;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Component;

import com.lalke.mikroservisnaarhisbackend.model.Speaker;
import com.lalke.mikroservisnaarhisbackend.repository.SpeakerRepository;

import java.util.List;

@Component
@RequiredArgsConstructor
public class SpeakersMQHandler {
    private final SpeakerRepository repository;

    @RabbitListener(queues = "speaker.get.all")
    @SendTo("speaker.get.all.res")
    public List<Speaker> handleGetAll(java.util.Map<String, Object> payload) {
        return repository.findAll();
    }

    @RabbitListener(queues = "speaker.get.id")
    @SendTo("speaker.get.id.res")
    public Speaker handleGetById(Long id) {
        return repository.findById(id).orElse(null);
    }

    @RabbitListener(queues = "speaker.save")
    public void handleSave(Speaker speaker) {
        repository.save(speaker);
    }

    @RabbitListener(queues = "speaker.delete")
    public void handleDelete(Long id) {
        repository.deleteById(id);
    }
}
