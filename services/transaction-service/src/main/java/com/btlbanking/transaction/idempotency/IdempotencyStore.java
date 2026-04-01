package com.btlbanking.transaction.idempotency;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class IdempotencyStore {

  private final StringRedisTemplate redisTemplate;

  public IdempotencyStore(StringRedisTemplate redisTemplate) {
    this.redisTemplate = redisTemplate;
  }

  public Optional<IdempotencyRecord> find(String userId, String idempotencyKey) {
    String redisValue = redisTemplate.opsForValue().get(redisKey(userId, idempotencyKey));
    if (redisValue == null) {
      return Optional.empty();
    }
    return Optional.of(IdempotencyRecord.fromRedisValue(userId, idempotencyKey, redisValue));
  }

  public IdempotencyRecord save(String userId, String idempotencyKey, String requestFingerprint,
      UUID transferId) {
    IdempotencyRecord record = new IdempotencyRecord(userId, idempotencyKey, requestFingerprint,
        transferId);
    redisTemplate.opsForValue().set(redisKey(userId, idempotencyKey), record.toRedisValue());
    return record;
  }

  private String redisKey(String userId, String idempotencyKey) {
    return "transaction:idempotency:" + userId + ":" + idempotencyKey;
  }
}
