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
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TransferApplicationService {

  private final TransferRepository repository;
  private final FraudClient fraudClient;
  private final AccountClient accountClient;
  private final CircuitBreaker fraudCircuitBreaker;
  private final CircuitBreaker accountCircuitBreaker;
  private final IdempotencyStore idempotencyStore;
  private final OutboxPublisher outboxPublisher;

  public TransferApplicationService(TransferRepository repository,
      FraudClient fraudClient,
      AccountClient accountClient,
      @Qualifier("fraudCircuitBreaker") CircuitBreaker fraudCircuitBreaker,
      @Qualifier("accountCircuitBreaker") CircuitBreaker accountCircuitBreaker,
      IdempotencyStore idempotencyStore,
      OutboxPublisher outboxPublisher) {
    this.repository = repository;
    this.fraudClient = fraudClient;
    this.accountClient = accountClient;
    this.fraudCircuitBreaker = fraudCircuitBreaker;
    this.accountCircuitBreaker = accountCircuitBreaker;
    this.idempotencyStore = idempotencyStore;
    this.outboxPublisher = outboxPublisher;
  }

  public TransferResponse create(String userId, String idempotencyKey, CreateTransferRequest request) {
    String requestFingerprint = fingerprint(request);
    Optional<IdempotencyRecord> existing = idempotencyStore.find(userId, idempotencyKey);
    if (existing.isPresent()) {
      IdempotencyRecord record = existing.get();
      if (!record.matches(userId, idempotencyKey, requestFingerprint)) {
        throw new IllegalArgumentException("Idempotency key reused with a different request payload");
      }
      TransferEntity transfer = repository.findById(record.transferId())
          .orElseThrow(() -> new IllegalStateException("Missing transfer for idempotency record"));
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
    return toResponse(entity);
  }

  @Transactional(readOnly = true)
  public TransferResponse get(UUID transferId) {
    TransferEntity entity = repository.findById(transferId)
        .orElseThrow(() -> new TransferNotFoundException("Transfer not found: " + transferId));
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
}
