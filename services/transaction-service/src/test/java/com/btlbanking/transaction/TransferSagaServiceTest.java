package com.btlbanking.transaction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.btlbanking.transaction.client.AccountClient;
import com.btlbanking.transaction.client.FraudCheckResponse;
import com.btlbanking.transaction.client.FraudClient;
import com.btlbanking.transaction.domain.TransferEntity;
import com.btlbanking.transaction.domain.TransferRepository;
import com.btlbanking.transaction.domain.TransferStatus;
import com.btlbanking.transaction.idempotency.IdempotencyStore;
import com.btlbanking.transaction.outbox.OutboxPublisher;
import com.btlbanking.transaction.service.TransferApplicationService;
import com.btlbanking.transaction.web.CreateTransferRequest;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TransferSagaServiceTest {

  @Mock
  AccountClient accountClient;

  @Mock
  FraudClient fraudClient;

  @Mock
  TransferRepository transferRepository;

  @Mock
  IdempotencyStore idempotencyStore;

  @Mock
  OutboxPublisher outboxPublisher;

  TransferApplicationService service;
  List<TransferStatus> savedStatuses;

  @BeforeEach
  void setUp() {
    savedStatuses = new ArrayList<>();
    when(transferRepository.save(any())).thenAnswer(invocation -> {
      TransferEntity entity = invocation.getArgument(0);
      savedStatuses.add(entity.getStatus());
      if (entity.getId() == null) {
        entity.setId(UUID.randomUUID());
      }
      return entity;
    });
    when(idempotencyStore.find(any(), any())).thenReturn(Optional.empty());
    CircuitBreakerRegistry registry = CircuitBreakerRegistry.ofDefaults();
    service = new TransferApplicationService(
        transferRepository,
        fraudClient,
        accountClient,
        registry.circuitBreaker("fraud-client"),
        registry.circuitBreaker("account-client"),
        idempotencyStore,
        outboxPublisher);
  }

  @Test
  void rejected_fraud_sets_status_rejected() {
    when(fraudClient.check(any())).thenReturn(new FraudCheckResponse("REJECTED", "AMOUNT_LIMIT_EXCEEDED"));

    var response = service.create("user-1", "idem-1",
        new CreateTransferRequest("100001", "200001", new BigDecimal("500")));

    assertThat(response.status()).isEqualTo(TransferStatus.REJECTED);
    verify(accountClient, org.mockito.Mockito.never()).debit(any(), any());
    assertThat(savedStatuses).containsExactly(TransferStatus.PENDING, TransferStatus.REJECTED);
    verify(outboxPublisher).record(any());
  }

  @Test
  void debit_failure_sets_status_failed() {
    when(fraudClient.check(any())).thenReturn(new FraudCheckResponse("APPROVED", "OK"));
    doThrow(new RuntimeException("debit down")).when(accountClient)
        .debit("100001", new BigDecimal("500"));

    var response = service.create("user-1", "idem-2",
        new CreateTransferRequest("100001", "200001", new BigDecimal("500")));

    assertThat(response.status()).isEqualTo(TransferStatus.FAILED);
    verify(accountClient).debit("100001", new BigDecimal("500"));
    assertThat(savedStatuses).containsExactly(TransferStatus.PENDING, TransferStatus.FAILED);
    verify(outboxPublisher).record(any());
  }

  @Test
  void credit_failure_triggers_compensation_and_sets_compensated() {
    when(fraudClient.check(any())).thenReturn(new FraudCheckResponse("APPROVED", "OK"));
    doNothing().when(accountClient).debit("100001", new BigDecimal("500"));
    doThrow(new RuntimeException("credit down")).when(accountClient)
        .credit("200001", new BigDecimal("500"));

    var response = service.create("user-1", "idem-3",
        new CreateTransferRequest("100001", "200001", new BigDecimal("500")));

    assertThat(response.status()).isEqualTo(TransferStatus.COMPENSATED);
    verify(accountClient).compensate("100001", new BigDecimal("500"));
    assertThat(savedStatuses).containsExactly(TransferStatus.PENDING, TransferStatus.COMPENSATED);
    verify(outboxPublisher).record(any());
  }

  @Test
  void success_flow_sets_status_success() {
    when(fraudClient.check(any())).thenReturn(new FraudCheckResponse("APPROVED", "OK"));

    var response = service.create("user-1", "idem-4",
        new CreateTransferRequest("100001", "200001", new BigDecimal("500")));

    assertThat(response.status()).isEqualTo(TransferStatus.SUCCESS);
    verify(accountClient).debit("100001", new BigDecimal("500"));
    verify(accountClient).credit("200001", new BigDecimal("500"));
    assertThat(savedStatuses).containsExactly(TransferStatus.PENDING, TransferStatus.SUCCESS);
    verify(outboxPublisher).record(any());
  }
}
