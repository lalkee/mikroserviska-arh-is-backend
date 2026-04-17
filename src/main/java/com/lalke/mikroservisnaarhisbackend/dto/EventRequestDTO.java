package com.lalke.mikroservisnaarhisbackend.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class EventRequestDTO {
    private Long id;
    private String name;
    private String agenda;
    private LocalDateTime dateTime;
    private String duration;
    private double registrationFee;
    private Long locationId; // ID for the ManyToOne relationship
    private List<Long> speakerIds; // IDs to be sent to the Speaker service
}