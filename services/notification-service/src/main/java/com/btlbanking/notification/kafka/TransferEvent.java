package com.btlbanking.notification.kafka;

import java.math.BigDecimal;
import com.btlbanking.notification.domain.TransferStatus;
import java.time.Instant;
import java.util.UUID;

public record TransferEvent(
    UUID eventId,
    UUID transferId,
    String eventType,
    TransferStatus status,
    String userId,
    String sourceAccount,
    String destinationAccount,
    BigDecimal amount,
    String idempotencyKey,
    Instant occurredAt
) {
}
