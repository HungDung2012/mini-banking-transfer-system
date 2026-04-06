package com.btlbanking.transaction.outbox;

import com.btlbanking.transaction.events.TransferEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class OutboxPublisher {

  private final OutboxEventRepository repository;
  private final ObjectMapper objectMapper;
  private final ObjectProvider<KafkaTemplate<String, String>> kafkaTemplateProvider;

  public OutboxPublisher(OutboxEventRepository repository, ObjectMapper objectMapper,
      ObjectProvider<KafkaTemplate<String, String>> kafkaTemplateProvider) {
    this.repository = repository;
    this.objectMapper = objectMapper;
    this.kafkaTemplateProvider = kafkaTemplateProvider;
  }

  public OutboxEventEntity record(TransferEvent event) {
    OutboxEventEntity entity = new OutboxEventEntity();
    entity.setAggregateId(event.transferId());
    entity.setEventType(event.eventType());
    entity.setPayload(serialize(event));
    entity.setPublished(false);
    return repository.save(entity);
  }

  @Scheduled(fixedDelay = 2000L)
  public void publishPendingEvents() {
    KafkaTemplate<String, String> kafkaTemplate = kafkaTemplateProvider.getIfAvailable();
    if (kafkaTemplate == null) {
      return;
    }

    List<OutboxEventEntity> pending = repository.findTop50ByPublishedFalseOrderByCreatedAtAsc();
    for (OutboxEventEntity event : pending) {
      kafkaTemplate.send("transfer-events", event.getAggregateId().toString(), event.getPayload());
      event.markPublished();
      repository.save(event);
    }
  }

  private String serialize(TransferEvent event) {
    try {
      return objectMapper.writeValueAsString(event);
    } catch (JsonProcessingException ex) {
      throw new IllegalStateException("Unable to serialize outbox event", ex);
    }
  }
}
