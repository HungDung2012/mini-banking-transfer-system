package com.btlbanking.audit;

import static org.assertj.core.api.Assertions.assertThat;

import com.btlbanking.audit.domain.AuditEventRepository;
import com.btlbanking.audit.domain.ProcessedAuditEventRepository;
import com.btlbanking.audit.kafka.TransferAuditConsumer;
import com.btlbanking.audit.kafka.TransferEvent;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
    "spring.kafka.listener.auto-startup=false",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class TransferAuditConsumerTest {

  @Autowired
  TransferAuditConsumer consumer;

  @Autowired
  AuditEventRepository auditEventRepository;

  @Autowired
  ProcessedAuditEventRepository processedAuditEventRepository;

  @Test
  void duplicate_event_creates_one_audit_record() {
    var event = new TransferEvent(UUID.randomUUID(), "TRANSFER_COMPENSATED",
        UUID.randomUUID(), "COMPENSATED");

    consumer.consume(event);
    consumer.consume(event);

    assertThat(auditEventRepository.count()).isEqualTo(1);
    assertThat(processedAuditEventRepository.count()).isEqualTo(1);
  }
}