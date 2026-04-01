package com.btlbanking.transaction.idempotency;

import java.util.UUID;

public record IdempotencyRecord(String userId, String idempotencyKey, String requestFingerprint,
    UUID transferId) {

  public boolean matches(String userId, String idempotencyKey, String requestFingerprint) {
    return this.userId.equals(userId)
        && this.idempotencyKey.equals(idempotencyKey)
        && this.requestFingerprint.equals(requestFingerprint);
  }

  public String toRedisValue() {
    return requestFingerprint + "::" + transferId;
  }

  public static IdempotencyRecord fromRedisValue(String userId, String idempotencyKey,
      String redisValue) {
    String[] parts = redisValue.split("::", 2);
    if (parts.length != 2) {
      throw new IllegalArgumentException("Invalid idempotency record");
    }
    return new IdempotencyRecord(userId, idempotencyKey, parts[0], UUID.fromString(parts[1]));
  }
}
