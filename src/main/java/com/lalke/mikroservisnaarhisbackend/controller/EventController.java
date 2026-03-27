package com.lalke.mikroservisnaarhisbackend.controller;

import com.lalke.mikroservisnaarhisbackend.model.Event;
import com.lalke.mikroservisnaarhisbackend.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {
    private final EventRepository eventRepository;

    @GetMapping
    public List<Event> getAll() {
        return eventRepository.findAll();
    }

    @PostMapping
    public Event create(@RequestBody Event event) {
        return eventRepository.save(event);
    }

    @PutMapping("/{id}")
    public Event update(@PathVariable Long id, @RequestBody Event event) {
        event.setId(id);
        return eventRepository.save(event);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        eventRepository.deleteById(id);
    }
}
