package com.lalke.mikroservisnaarhisbackend.controller;

import com.lalke.mikroservisnaarhisbackend.model.Location;
import com.lalke.mikroservisnaarhisbackend.repository.LocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/locations")
@RequiredArgsConstructor
public class LocationController {
    private final LocationRepository locationRepository;

    @GetMapping
    public List<Location> getAll() {
        return locationRepository.findAll();
    }

    @PostMapping
    public Location create(@RequestBody Location location) {
        return locationRepository.save(location);
    }
}
