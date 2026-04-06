package com.btlbanking.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.btlbanking.notification.domain.NotificationRepository;
import com.btlbanking.notification.domain.ProcessedEventRepository;
import com.btlbanking.notification.kafka.TransferEvent;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
    "spring.kafka.listener.auto-startup=false",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class TransferNotificationConsumerTest {

  @Autowired
  NotificationRepository notificationRepository;

  @Autowired
  ProcessedEventRepository processedEventRepository;

  @Autowired
  com.btlbanking.notification.kafka.TransferNotificationConsumer consumer;

  @Autowired
  MockMvc mockMvc;

  @BeforeEach
  void cleanDatabase() {
    notificationRepository.deleteAll();
    processedEventRepository.deleteAll();
  }

  @Test
  void duplicate_event_is_processed_once() {
    var event = new TransferEvent(
        UUID.randomUUID(),
        UUID.randomUUID(),
        "TRANSFER_COMPLETED",
        com.btlbanking.notification.domain.TransferStatus.SUCCESS,
        "alice",
        "100001",
        "200001",
        new BigDecimal("50"),
        "idem-1",
        Instant.now());

    consumer.consume(event);
    consumer.consume(event);

    assertThat(notificationRepository.count()).isEqualTo(2);
    assertThat(processedEventRepository.count()).isEqualTo(1);
  }

  @Test
  void get_notifications_returns_incoming_and_outgoing_notifications_for_the_account() throws Exception {
    consumer.consume(new TransferEvent(
        UUID.randomUUID(),
        UUID.randomUUID(),
        "TRANSFER_COMPLETED",
        com.btlbanking.notification.domain.TransferStatus.SUCCESS,
        "alice",
        "100001",
        "200001",
        new BigDecimal("50"),
        "idem-2",
        Instant.now()));

    consumer.consume(new TransferEvent(
        UUID.randomUUID(),
        UUID.randomUUID(),
        "TRANSFER_REJECTED",
        com.btlbanking.notification.domain.TransferStatus.REJECTED,
        "alice",
        "100001",
        "300001",
        new BigDecimal("999"),
        "idem-3",
        Instant.now()));

    mockMvc.perform(get("/notifications/account/200001"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].recipientAccount").value("200001"))
        .andExpect(jsonPath("$[0].title").value("Incoming transfer"))
        .andExpect(jsonPath("$[0].body").value("Ban vua nhan 50 VND tu tai khoan 100001"));

    mockMvc.perform(get("/notifications/account/100001"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].recipientAccount").value("100001"))
        .andExpect(jsonPath("$[0].title").value("Outgoing transfer"))
        .andExpect(jsonPath("$[0].body").value("Ban vua chuyen 50 VND den tai khoan 200001"));
  }
}
