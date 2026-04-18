package com.lalke.mikroservisnaarhisbackend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lalke.mikroservisnaarhisbackend.model.OutboxRecord;

public interface OutboxRepository extends JpaRepository<OutboxRecord, Long>{

    List<OutboxRecord> findTop50ByOrderByTimestampAsc();
    
}
