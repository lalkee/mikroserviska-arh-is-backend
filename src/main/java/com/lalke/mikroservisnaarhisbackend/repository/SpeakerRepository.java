package com.lalke.mikroservisnaarhisbackend.repository;

import com.lalke.mikroservisnaarhisbackend.model.Speaker;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpeakerRepository extends JpaRepository<Speaker, Long> {
}
