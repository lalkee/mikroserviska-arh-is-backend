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

    @PostMapping
    public Speaker create(@RequestBody Speaker speaker) {
        return speakerRepository.save(speaker);
    }
}
