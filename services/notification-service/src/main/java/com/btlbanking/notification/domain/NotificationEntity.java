package com.btlbanking.notification.domain;

import com.btlbanking.notification.kafka.TransferEvent;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Entity
@Table(name = "notifications")
public class NotificationEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "event_id", nullable = false, unique = true)
  private UUID eventId;

  @Column(name = "transfer_id", nullable = false)
  private UUID transferId;

  @Column(name = "event_type", nullable = false)
  private String eventType;

  @Column(nullable = false)
  private String status;

  @Column(nullable = false)
  private String message;

  @Column(name = "recipient_account", nullable = false)
  private String recipientAccount;

  @Column(name = "source_account")
  private String sourceAccount;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  public static NotificationEntity incoming(TransferEvent event) {
    NotificationEntity entity = new NotificationEntity();
    entity.setEventId(scopedEventId(event.eventId(), "incoming"));
    entity.setTransferId(event.transferId());
    entity.setEventType(event.eventType());
    entity.setStatus(event.status().name());
    entity.setRecipientAccount(event.destinationAccount());
    entity.setSourceAccount(event.sourceAccount());
    entity.setMessage("Ban vua nhan " + event.amount().stripTrailingZeros().toPlainString()
        + " VND tu tai khoan " + event.sourceAccount());
    entity.setCreatedAt(Instant.now());
    return entity;
  }

  public static NotificationEntity outgoing(TransferEvent event) {
    NotificationEntity entity = new NotificationEntity();
    entity.setEventId(scopedEventId(event.eventId(), "outgoing"));
    entity.setTransferId(event.transferId());
    entity.setEventType(event.eventType());
    entity.setStatus(event.status().name());
    entity.setRecipientAccount(event.sourceAccount());
    entity.setSourceAccount(event.destinationAccount());
    entity.setMessage("Ban vua chuyen " + event.amount().stripTrailingZeros().toPlainString()
        + " VND den tai khoan " + event.destinationAccount());
    entity.setCreatedAt(Instant.now());
    return entity;
  }

  private static UUID scopedEventId(UUID eventId, String scope) {
    return UUID.nameUUIDFromBytes((eventId + ":" + scope).getBytes(StandardCharsets.UTF_8));
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public UUID getEventId() {
    return eventId;
  }

  public void setEventId(UUID eventId) {
    this.eventId = eventId;
  }

  public UUID getTransferId() {
    return transferId;
  }

  public void setTransferId(UUID transferId) {
    this.transferId = transferId;
  }

  public String getEventType() {
    return eventType;
  }

  public void setEventType(String eventType) {
    this.eventType = eventType;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public String getRecipientAccount() {
    return recipientAccount;
  }

  public void setRecipientAccount(String recipientAccount) {
    this.recipientAccount = recipientAccount;
  }

  public String getSourceAccount() {
    return sourceAccount;
  }

  public void setSourceAccount(String sourceAccount) {
    this.sourceAccount = sourceAccount;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }
}
