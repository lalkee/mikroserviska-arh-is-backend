package com.lalke.mikroservisnaarhisbackend.controller;

import com.lalke.mikroservisnaarhisbackend.model.Event;
import com.lalke.mikroservisnaarhisbackend.patterns.CircuitBreaker;
import com.lalke.mikroservisnaarhisbackend.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(path="/api/events", produces="application/json")
@RequiredArgsConstructor
public class EventController {
    private final EventRepository eventRepository;
    private final CircuitBreaker circuitBreaker;

    @GetMapping
    public List<Event> getAll() {
        return circuitBreaker.execute(() -> {
            return eventRepository.findAll();});
    }

    @GetMapping("/{id}")
    public Event getById(@PathVariable Long id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event not found"));
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