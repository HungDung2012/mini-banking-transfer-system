package com.btlbanking.notification.web;

import com.btlbanking.notification.domain.NotificationEntity;
import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
    UUID id,
    UUID transferId,
    String recipientAccount,
    String sourceAccount,
    String title,
    String body,
    String status,
    Instant createdAt
) {
  public static NotificationResponse from(NotificationEntity entity) {
    return new NotificationResponse(
        entity.getId(),
        entity.getTransferId(),
        entity.getRecipientAccount(),
        entity.getSourceAccount(),
        entity.getTitle(),
        entity.getMessage(),
        entity.getStatus(),
        entity.getCreatedAt());
  }
}
