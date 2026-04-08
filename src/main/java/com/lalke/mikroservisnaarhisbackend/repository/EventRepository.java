package com.lalke.mikroservisnaarhisbackend.repository;

import com.lalke.mikroservisnaarhisbackend.model.Event;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {

    @EntityGraph(attributePaths = {"location", "speakers"})
    @Query("select e from Event e")
    List<Event> findAllWithDetails();

    @EntityGraph(attributePaths = {"location", "speakers"})
    @Query("select e from Event e where e.id = :id") // Explicit query for find by ID
    Optional<Event> findByIdWithDetails(Long id);
}
