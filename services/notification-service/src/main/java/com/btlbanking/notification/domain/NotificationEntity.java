package com.btlbanking.notification.domain;

import com.btlbanking.notification.kafka.TransferEvent;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
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

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  public static NotificationEntity from(TransferEvent event) {
    NotificationEntity entity = new NotificationEntity();
    entity.setEventId(event.eventId());
    entity.setTransferId(event.transferId());
    entity.setEventType(event.eventType());
    entity.setStatus(event.status());
    entity.setMessage("Transfer " + event.status() + " for " + event.transferId());
    entity.setCreatedAt(Instant.now());
    return entity;
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

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }
}