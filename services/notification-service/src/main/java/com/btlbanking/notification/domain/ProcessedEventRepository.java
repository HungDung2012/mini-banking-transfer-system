package com.btlbanking.notification.domain;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEventEntity, UUID> {

  boolean existsByEventId(UUID eventId);
}