package com.btlbanking.transaction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.btlbanking.transaction.client.AccountClient;
import com.btlbanking.transaction.client.FraudCheckResponse;
import com.btlbanking.transaction.client.FraudClient;
import com.btlbanking.transaction.domain.TransferRepository;
import com.btlbanking.transaction.outbox.OutboxEventRepository;
import com.btlbanking.transaction.outbox.OutboxPublisher;
import com.btlbanking.transaction.web.CreateTransferRequest;
import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.kafka.core.KafkaTemplate;

@SpringBootTest
@AutoConfigureMockMvc
class IdempotencyAndOutboxIT {

  @Autowired
  com.btlbanking.transaction.service.TransferApplicationService service;

  @Autowired
  TransferRepository transferRepository;

  @Autowired
  OutboxEventRepository outboxRepository;

  @Autowired
  OutboxPublisher outboxPublisher;

  @MockBean
  FraudClient fraudClient;

  @MockBean
  AccountClient accountClient;

  @MockBean
  KafkaTemplate<String, String> kafkaTemplate;

  @MockBean
  StringRedisTemplate redisTemplate;

  private ValueOperations<String, String> valueOperations;
  private final Map<String, String> redisStore = new ConcurrentHashMap<>();

  @BeforeEach
  void setUpRedisMock() {
    valueOperations = mock(ValueOperations.class);
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    when(valueOperations.get(any())).thenAnswer(invocation -> redisStore.get(invocation.getArgument(0)));
    doAnswer(invocation -> {
      redisStore.put(invocation.getArgument(0), invocation.getArgument(1));
      return null;
    }).when(valueOperations).set(any(), any());
  }

  @AfterEach
  void cleanUp() {
    redisStore.clear();
    outboxRepository.deleteAll();
    transferRepository.deleteAll();
  }

  @Test
  void duplicate_idempotency_key_returns_same_transfer_and_writes_one_outbox_record() {
    when(fraudClient.check(any())).thenReturn(new FraudCheckResponse("APPROVED", "OK"));
    doNothing().when(accountClient).debit("100001", new BigDecimal("500"));
    doNothing().when(accountClient).credit("200001", new BigDecimal("500"));

    var request = new CreateTransferRequest("100001", "200001", new BigDecimal("500"));
    var first = service.create("user-1", "idem-1", request);
    var second = service.create("user-1", "idem-1", request);

    assertThat(second.transferId()).isEqualTo(first.transferId());
    assertThat(second.status()).isEqualTo(first.status());
    assertThat(transferRepository.count()).isEqualTo(1);
    assertThat(outboxRepository.findAll()).hasSize(1);
  }

  @Test
  void outbox_publisher_sends_pending_event_and_marks_it_published() {
    when(fraudClient.check(any())).thenReturn(new FraudCheckResponse("APPROVED", "OK"));
    doNothing().when(accountClient).debit("100001", new BigDecimal("500"));
    doNothing().when(accountClient).credit("200001", new BigDecimal("500"));

    var response = service.create("user-2", "idem-2",
        new CreateTransferRequest("100001", "200001", new BigDecimal("500")));

    outboxPublisher.publishPendingEvents();

    var event = outboxRepository.findAll().getFirst();
    verify(kafkaTemplate).send("transfer-events", response.transferId().toString(), event.getPayload());
    assertThat(outboxRepository.findAll()).allMatch(outbox -> outbox.isPublished());
  }
}
