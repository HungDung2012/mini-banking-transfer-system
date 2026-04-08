package com.btlbanking.notification.kafka;

import com.btlbanking.notification.domain.NotificationEntity;
import com.btlbanking.notification.domain.NotificationRepository;
import com.btlbanking.notification.domain.ProcessedEventEntity;
import com.btlbanking.notification.domain.ProcessedEventRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.annotation.Observed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class TransferNotificationConsumer {
  private static final Logger log = LoggerFactory.getLogger(TransferNotificationConsumer.class);

  private final ProcessedEventRepository processedEventRepository;
  private final NotificationRepository notificationRepository;
  private final MeterRegistry meterRegistry;

  public TransferNotificationConsumer(ProcessedEventRepository processedEventRepository,
      NotificationRepository notificationRepository,
      MeterRegistry meterRegistry) {
    this.processedEventRepository = processedEventRepository;
    this.notificationRepository = notificationRepository;
    this.meterRegistry = meterRegistry;
  }

  @Observed(name = "banking.notification.consume", contextualName = "notification-consume")
  @KafkaListener(topics = "transfer-events", groupId = "notification-service")
  @Transactional
  public void consume(TransferEvent event) {
    if (processedEventRepository.existsByEventId(event.eventId())) {
      meterRegistry.counter("banking.notification.events", "result", "duplicate").increment();
      return;
    }

    if (!"TRANSFER_COMPLETED".equals(event.eventType())) {
      meterRegistry.counter("banking.notification.events", "result", "ignored").increment();
      processedEventRepository.save(new ProcessedEventEntity(event.eventId(), event.transferId()));
      return;
    }

    notificationRepository.save(NotificationEntity.incoming(event));
    notificationRepository.save(NotificationEntity.outgoing(event));
    processedEventRepository.save(new ProcessedEventEntity(event.eventId(), event.transferId()));
    meterRegistry.counter("banking.notification.events", "result", "processed").increment();
    log.info("Stored notifications transferId={} sourceAccount={} destinationAccount={} amount={}",
        event.transferId(), event.sourceAccount(), event.destinationAccount(), event.amount());
  }
}
