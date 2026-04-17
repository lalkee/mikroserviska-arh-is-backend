package com.lalke.mikroservisnaarhisbackend.dto;

import java.time.LocalDateTime;
import java.util.List;
import com.lalke.mikroservisnaarhisbackend.model.Event;
import com.lalke.mikroservisnaarhisbackend.model.Location;
import com.lalke.mikroservisnaarhisbackend.model.Speaker;

import lombok.Data;

@Data
public class EventResponseDTO {
    private Long id;
    private String name;
    private String agenda;
    private LocalDateTime dateTime;
    private String duration;
    private double registrationFee;
    private Location location;
    private List<Speaker> speakers;

    /**
     * Maps the Event entity and the list of Speakers fetched from the 
     * external service into a single response object.
     */
    public static EventResponseDTO from(Event event, List<Speaker> speakers) {
        EventResponseDTO dto = new EventResponseDTO();
        dto.setId(event.getId());
        dto.setName(event.getName());
        dto.setAgenda(event.getAgenda());
        dto.setDateTime(event.getDateTime());
        dto.setDuration(event.getDuration());
        dto.setRegistrationFee(event.getRegistrationFee());
        dto.setLocation(event.getLocation());
        dto.setSpeakers(speakers);
        return dto;
    }
}