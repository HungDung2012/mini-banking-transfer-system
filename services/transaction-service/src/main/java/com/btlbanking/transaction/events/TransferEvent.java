package com.btlbanking.transaction.events;

import com.btlbanking.transaction.domain.TransferEntity;
import com.btlbanking.transaction.domain.TransferStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransferEvent(UUID eventId, UUID transferId, String eventType, TransferStatus status,
    String userId, String sourceAccount, String destinationAccount, BigDecimal amount,
    String idempotencyKey, Instant occurredAt) {

  public static TransferEvent from(TransferEntity entity) {
    return new TransferEvent(
        UUID.randomUUID(),
        entity.getId(),
        eventTypeFor(entity.getStatus()),
        entity.getStatus(),
        entity.getUserId(),
        entity.getSourceAccount(),
        entity.getDestinationAccount(),
        entity.getAmount(),
        entity.getIdempotencyKey(),
        Instant.now());
  }

  private static String eventTypeFor(TransferStatus status) {
    return switch (status) {
      case SUCCESS -> "TRANSFER_COMPLETED";
      case FAILED -> "TRANSFER_FAILED";
      case COMPENSATED -> "TRANSFER_COMPENSATED";
      case REJECTED -> "TRANSFER_REJECTED";
      case PENDING -> "TRANSFER_PENDING";
    };
  }
}
