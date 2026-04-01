package com.btlbanking.notification;

import static org.assertj.core.api.Assertions.assertThat;

import com.btlbanking.notification.domain.NotificationRepository;
import com.btlbanking.notification.domain.ProcessedEventRepository;
import com.btlbanking.notification.kafka.TransferEvent;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
    "spring.kafka.listener.auto-startup=false",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class TransferNotificationConsumerTest {

  @Autowired
  NotificationRepository notificationRepository;

  @Autowired
  ProcessedEventRepository processedEventRepository;

  @Autowired
  com.btlbanking.notification.kafka.TransferNotificationConsumer consumer;

  @Test
  void duplicate_event_is_processed_once() {
    var event = new TransferEvent(UUID.randomUUID(), "TRANSFER_COMPLETED", UUID.randomUUID(), "SUCCESS");

    consumer.consume(event);
    consumer.consume(event);

    assertThat(notificationRepository.count()).isEqualTo(1);
    assertThat(processedEventRepository.count()).isEqualTo(1);
  }
}