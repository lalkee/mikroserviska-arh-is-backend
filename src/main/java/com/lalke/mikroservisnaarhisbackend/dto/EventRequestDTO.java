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
    private Long locationId;
    private List<Long> speakerIds;
}