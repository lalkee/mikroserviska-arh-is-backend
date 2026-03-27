package com.lalke.mikroservisnaarhisbackend.repository;

import com.lalke.mikroservisnaarhisbackend.model.Event;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRepository extends JpaRepository<Event, Long> {
}
