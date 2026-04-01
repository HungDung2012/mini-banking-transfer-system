package com.btlbanking.notification.kafka;

import java.util.UUID;

public record TransferEvent(UUID eventId, String eventType, UUID transferId, String status) {
}