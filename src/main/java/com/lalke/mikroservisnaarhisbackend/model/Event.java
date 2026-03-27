package com.lalke.mikroservisnaarhisbackend.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String name;
    private String agenda;
    private LocalDateTime dateTime;
    private String duration;
    private double registrationFee;
    
    @ManyToOne
    private Location location;
    
    @ManyToMany
    private List<Speaker> speakers;
}
