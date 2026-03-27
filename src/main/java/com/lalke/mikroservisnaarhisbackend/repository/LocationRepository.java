package com.lalke.mikroservisnaarhisbackend.repository;

import com.lalke.mikroservisnaarhisbackend.model.Location;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LocationRepository extends JpaRepository<Location, Long> {
}
