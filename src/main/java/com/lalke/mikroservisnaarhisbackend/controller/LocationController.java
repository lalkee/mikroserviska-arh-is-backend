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

    @GetMapping("/{id}")
    public Location getById(@PathVariable Long id) {
        return locationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Location not found"));
    }

    @PostMapping
    public Location create(@RequestBody Location location) {
        return locationRepository.save(location);
    }

    @PutMapping("/{id}")
    public Location update(@PathVariable Long id, @RequestBody Location location) {
        location.setId(id);
        return locationRepository.save(location);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        locationRepository.deleteById(id);
    }
}