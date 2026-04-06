package com.btlbanking.notification.kafka;

import com.btlbanking.notification.domain.NotificationEntity;
import com.btlbanking.notification.domain.NotificationRepository;
import com.btlbanking.notification.domain.ProcessedEventEntity;
import com.btlbanking.notification.domain.ProcessedEventRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class TransferNotificationConsumer {

  private final ProcessedEventRepository processedEventRepository;
  private final NotificationRepository notificationRepository;

  public TransferNotificationConsumer(ProcessedEventRepository processedEventRepository,
      NotificationRepository notificationRepository) {
    this.processedEventRepository = processedEventRepository;
    this.notificationRepository = notificationRepository;
  }

  @KafkaListener(topics = "transfer-events", groupId = "notification-service")
  @Transactional
  public void consume(TransferEvent event) {
    if (processedEventRepository.existsByEventId(event.eventId())) {
      return;
    }

    if (!"TRANSFER_COMPLETED".equals(event.eventType())) {
      processedEventRepository.save(new ProcessedEventEntity(event.eventId(), event.transferId()));
      return;
    }

    notificationRepository.save(NotificationEntity.incoming(event));
    notificationRepository.save(NotificationEntity.outgoing(event));
    processedEventRepository.save(new ProcessedEventEntity(event.eventId(), event.transferId()));
  }
}
