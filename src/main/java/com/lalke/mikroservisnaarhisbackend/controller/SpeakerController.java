package com.lalke.mikroservisnaarhisbackend.controller;

import com.lalke.mikroservisnaarhisbackend.model.Speaker;
import com.lalke.mikroservisnaarhisbackend.repository.SpeakerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/speakers")
@RequiredArgsConstructor
public class SpeakerController {
    private final SpeakerRepository speakerRepository;

    @GetMapping
    public List<Speaker> getAll() {
        return speakerRepository.findAll();
    }

    @GetMapping("/{id}")
    public Speaker getById(@PathVariable Long id) {
        return speakerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Speaker not found"));
    }

    @PostMapping
    public Speaker create(@RequestBody Speaker speaker) {
        return speakerRepository.save(speaker);
    }

    @PutMapping("/{id}")
    public Speaker update(@PathVariable Long id, @RequestBody Speaker speaker) {
        speaker.setId(id);
        return speakerRepository.save(speaker);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        speakerRepository.deleteById(id);
    }
}