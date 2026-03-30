package com.lalke.mikroservisnaarhisbackend.config;

import com.lalke.mikroservisnaarhisbackend.model.Event;
import com.lalke.mikroservisnaarhisbackend.model.Location;
import com.lalke.mikroservisnaarhisbackend.model.Speaker;
import com.lalke.mikroservisnaarhisbackend.repository.EventRepository;
import com.lalke.mikroservisnaarhisbackend.repository.LocationRepository;
import com.lalke.mikroservisnaarhisbackend.repository.SpeakerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final LocationRepository locationRepository;
    private final SpeakerRepository speakerRepository;
    private final EventRepository eventRepository;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        if (locationRepository.count() > 0 || speakerRepository.count() > 0 || eventRepository.count() > 0) {
            System.out.println("Database already contains data, skipping seeding.");
            return;
        }

        System.out.println("Seeding database...");

        // 1. Seed Locations
        Location location1 = new Location(null, "Main Conference Hall", "Zmaj Jovina 2, Belgrade", 300);
        Location location2 = new Location(null, "Innovation Center", "Bulevar Kralja Aleksandra 15, Belgrade", 100);
        locationRepository.saveAll(List.of(location1, location2));

        // 2. Seed Speakers
        Speaker speaker1 = new Speaker(null, "Marko", "Marković", "Senior Software Engineer", "Microservices & Cloud");
        Speaker speaker2 = new Speaker(null, "Jelena", "Jovanović", "Cloud Solutions Architect", "AWS & Kubernetes");
        Speaker speaker3 = new Speaker(null, "Nikola", "Nikolić", "Backend Developer", "Spring Boot Expert");
        speakerRepository.saveAll(List.of(speaker1, speaker2, speaker3));

        // 3. Seed Events
        Event event1 = new Event(
                null,
                "Microservices Masterclass",
                "Deep dive into building scalable microservices with Spring Boot and Docker.",
                LocalDateTime.now().plusDays(10).withHour(10).withMinute(0).withSecond(0),
                "6 hours",
                50.0,
                location1,
                List.of(speaker1, speaker3)
        );

        Event event2 = new Event(
                null,
                "Modern Java Development",
                "Exploring the latest Java features and functional programming patterns.",
                LocalDateTime.now().plusDays(15).withHour(14).withMinute(0).withSecond(0),
                "3 hours",
                30.0,
                location2,
                List.of(speaker3)
        );

        Event event3 = new Event(
                null,
                "Cloud-Native Summit",
                "Architecture and implementation of resilient cloud-native systems.",
                LocalDateTime.now().plusDays(20).withHour(9).withMinute(30).withSecond(0),
                "8 hours",
                100.0,
                location1,
                List.of(speaker1, speaker2)
        );

        eventRepository.saveAll(List.of(event1, event2, event3));

        System.out.println("Successfully seeded " + locationRepository.count() + " locations, " + 
                           speakerRepository.count() + " speakers, and " + 
                           eventRepository.count() + " events.");
    }
}
