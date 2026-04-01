package com.btlbanking.audit.kafka;

import com.btlbanking.audit.domain.AuditEventEntity;
import com.btlbanking.audit.domain.AuditEventRepository;
import com.btlbanking.audit.domain.ProcessedAuditEventEntity;
import com.btlbanking.audit.domain.ProcessedAuditEventRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class TransferAuditConsumer {

  private final ProcessedAuditEventRepository processedRepository;
  private final AuditEventRepository auditRepository;

  public TransferAuditConsumer(ProcessedAuditEventRepository processedRepository,
      AuditEventRepository auditRepository) {
    this.processedRepository = processedRepository;
    this.auditRepository = auditRepository;
  }

  @KafkaListener(topics = "transfer-events", groupId = "audit-service")
  @Transactional
  public void consume(TransferEvent event) {
    if (processedRepository.existsByEventId(event.eventId())) {
      return;
    }

    auditRepository.save(AuditEventEntity.from(event));
    processedRepository.save(new ProcessedAuditEventEntity(event.eventId(), event.transferId()));
  }
}