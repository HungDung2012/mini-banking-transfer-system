package com.btlbanking.audit.domain;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedAuditEventRepository extends JpaRepository<ProcessedAuditEventEntity, UUID> {

  boolean existsByEventId(UUID eventId);
}