package com.btlbanking.audit.kafka;

import com.btlbanking.audit.domain.AuditEventEntity;
import com.btlbanking.audit.domain.AuditEventRepository;
import com.btlbanking.audit.domain.ProcessedAuditEventEntity;
import com.btlbanking.audit.domain.ProcessedAuditEventRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.annotation.Observed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class TransferAuditConsumer {
  private static final Logger log = LoggerFactory.getLogger(TransferAuditConsumer.class);

  private final ProcessedAuditEventRepository processedRepository;
  private final AuditEventRepository auditRepository;
  private final MeterRegistry meterRegistry;

  public TransferAuditConsumer(ProcessedAuditEventRepository processedRepository,
      AuditEventRepository auditRepository,
      MeterRegistry meterRegistry) {
    this.processedRepository = processedRepository;
    this.auditRepository = auditRepository;
    this.meterRegistry = meterRegistry;
  }

  @Observed(name = "banking.audit.consume", contextualName = "audit-consume")
  @KafkaListener(topics = "transfer-events", groupId = "audit-service")
  @Transactional
  public void consume(TransferEvent event) {
    if (processedRepository.existsByEventId(event.eventId())) {
      meterRegistry.counter("banking.audit.events", "result", "duplicate").increment();
      return;
    }

    auditRepository.save(AuditEventEntity.from(event));
    processedRepository.save(new ProcessedAuditEventEntity(event.eventId(), event.transferId()));
    meterRegistry.counter("banking.audit.events", "result", "processed").increment();
    log.info("Stored audit event transferId={} eventType={} status={}",
        event.transferId(), event.eventType(), event.status());
  }
}
