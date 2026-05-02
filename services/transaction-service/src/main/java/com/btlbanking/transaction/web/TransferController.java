package com.btlbanking.transaction.web;

import com.btlbanking.transaction.service.TransferApplicationService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/transfers")
public class TransferController {

  private final TransferApplicationService service;

  public TransferController(TransferApplicationService service) {
    this.service = service;
  }

  @PostMapping
  public TransferResponse create(@RequestHeader("Idempotency-Key") String idempotencyKey,
      @RequestHeader(value = "X-User-Id", required = false) String userId,
      @RequestHeader(value = "X-Consumer-Username", required = false) String consumerUsername,
      @RequestBody CreateTransferRequest request) {
    String effectiveUserId = userId;
    if (effectiveUserId == null || effectiveUserId.isBlank()) {
      effectiveUserId = consumerUsername;
    }
    if (effectiveUserId == null || effectiveUserId.isBlank()) {
      effectiveUserId = "anonymous";
    }
    return service.create(effectiveUserId, idempotencyKey, request);
  }
}
