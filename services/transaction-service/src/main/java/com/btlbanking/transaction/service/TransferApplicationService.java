package com.btlbanking.transaction.service;

import com.btlbanking.transaction.client.AccountClient;
import com.btlbanking.transaction.client.FraudCheckResponse;
import com.btlbanking.transaction.client.FraudClient;
import com.btlbanking.transaction.domain.TransferEntity;
import com.btlbanking.transaction.domain.TransferRepository;
import com.btlbanking.transaction.domain.TransferStatus;
import com.btlbanking.transaction.events.TransferEvent;
import com.btlbanking.transaction.idempotency.IdempotencyRecord;
import com.btlbanking.transaction.idempotency.IdempotencyStore;
import com.btlbanking.transaction.outbox.OutboxPublisher;
import com.btlbanking.transaction.web.CreateTransferRequest;
import com.btlbanking.transaction.web.TransferResponse;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.observation.annotation.Observed;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TransferApplicationService {
  private static final Logger log = LoggerFactory.getLogger(TransferApplicationService.class);

  private final TransferRepository repository;
  private final FraudClient fraudClient;
  private final AccountClient accountClient;
  private final CircuitBreaker fraudCircuitBreaker;
  private final CircuitBreaker accountCircuitBreaker;
  private final IdempotencyStore idempotencyStore;
  private final OutboxPublisher outboxPublisher;
  private final MeterRegistry meterRegistry;

  public TransferApplicationService(TransferRepository repository,
      FraudClient fraudClient,
      AccountClient accountClient,
      @Qualifier("fraudCircuitBreaker") CircuitBreaker fraudCircuitBreaker,
      @Qualifier("accountCircuitBreaker") CircuitBreaker accountCircuitBreaker,
      IdempotencyStore idempotencyStore,
      OutboxPublisher outboxPublisher,
      MeterRegistry meterRegistry) {
    this.repository = repository;
    this.fraudClient = fraudClient;
    this.accountClient = accountClient;
    this.fraudCircuitBreaker = fraudCircuitBreaker;
    this.accountCircuitBreaker = accountCircuitBreaker;
    this.idempotencyStore = idempotencyStore;
    this.outboxPublisher = outboxPublisher;
    this.meterRegistry = meterRegistry;
  }

  @Observed(name = "banking.transfer.create", contextualName = "transfer-create")
  public TransferResponse create(String userId, String idempotencyKey, CreateTransferRequest request) {
    long startNanos = System.nanoTime();
    log.info("Creating transfer userId={} idempotencyKey={} sourceAccount={} destinationAccount={} amount={}",
        userId, idempotencyKey, request.sourceAccount(), request.destinationAccount(), request.amount());
        
    String requestFingerprint = fingerprint(request);
    Optional<IdempotencyRecord> existing = idempotencyStore.find(userId, idempotencyKey);
    if (existing.isPresent()) {
      IdempotencyRecord record = existing.get();
      if (!record.matches(userId, idempotencyKey, requestFingerprint)) {
        meterRegistry.counter("banking.transfer.requests", "result", "IDEMPOTENCY_CONFLICT").increment();
        throw new IllegalArgumentException("Idempotency key reused with a different request payload");
      }
      TransferEntity transfer = repository.findById(record.transferId())
          .orElseThrow(() -> new IllegalStateException("Missing transfer for idempotency record"));
      meterRegistry.counter("banking.transfer.requests", "result", "IDEMPOTENT_REPLAY").increment();
      recordTransferDuration(startNanos, "IDEMPOTENT_REPLAY");
      return toResponse(transfer);
    }

    TransferEntity entity = new TransferEntity();
    entity.setUserId(userId);
    entity.setIdempotencyKey(idempotencyKey);
    entity.setSourceAccount(request.sourceAccount());
    entity.setDestinationAccount(request.destinationAccount());
    entity.setAmount(request.amount());
    entity.setStatus(TransferStatus.PENDING);
    entity = repository.save(entity);

    idempotencyStore.save(userId, idempotencyKey, requestFingerprint, entity.getId());

    TransferStatus finalStatus;
    try {
      FraudCheckResponse fraudDecision = execute(fraudCircuitBreaker, () -> fraudClient.check(request));
      if ("REJECTED".equalsIgnoreCase(fraudDecision.decision())) {
        finalStatus = TransferStatus.REJECTED;
      } else {
        try {
          execute(accountCircuitBreaker, () -> {
            accountClient.debit(request.sourceAccount(), request.amount());
            return null;
          });
          try {
            execute(accountCircuitBreaker, () -> {
              accountClient.credit(request.destinationAccount(), request.amount());
              return null;
            });
            finalStatus = TransferStatus.SUCCESS;
          } catch (RuntimeException creditFailure) {
            try {
              execute(accountCircuitBreaker, () -> {
                accountClient.compensate(request.sourceAccount(), request.amount());
                return null;
              });
              finalStatus = TransferStatus.COMPENSATED;
            } catch (RuntimeException compensationFailure) {
              finalStatus = TransferStatus.FAILED;
            }
          }
        } catch (RuntimeException debitFailure) {
          finalStatus = TransferStatus.FAILED;
        }
      }
    } catch (RuntimeException fraudFailure) {
      finalStatus = TransferStatus.FAILED;
    }

    entity.setStatus(finalStatus);
    entity = repository.save(entity);
    outboxPublisher.record(TransferEvent.from(entity));
    meterRegistry.counter("banking.transfer.requests", "result", finalStatus.name()).increment();
    recordTransferDuration(startNanos, finalStatus.name());
    log.info("Completed transfer transferId={} status={} userId={} sourceAccount={} destinationAccount={} amount={}",
        entity.getId(), finalStatus, userId, request.sourceAccount(), request.destinationAccount(), request.amount());
    return toResponse(entity);
  }

  private TransferResponse toResponse(TransferEntity entity) {
    return new TransferResponse(entity.getId(), entity.getStatus());
  }

  private <T> T execute(CircuitBreaker circuitBreaker, Supplier<T> supplier) {
    return CircuitBreaker.decorateSupplier(circuitBreaker, supplier).get();
  }

  private String fingerprint(CreateTransferRequest request) {
    return request.sourceAccount() + "|" + request.destinationAccount() + "|"
        + request.amount().stripTrailingZeros().toPlainString();
  }

  private void recordTransferDuration(long startNanos, String result) {
    Timer.builder("banking.transfer.duration")
        .tag("result", result)
        .register(meterRegistry)
        .record(System.nanoTime() - startNanos, TimeUnit.NANOSECONDS);
  }
}
