package com.btlbanking.transaction.web;

import com.btlbanking.transaction.service.TransferApplicationService;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
      @RequestHeader("X-User-Id") String userId,
      @RequestBody CreateTransferRequest request) {
    return service.create(userId, idempotencyKey, request);
  }

  @GetMapping("/{transferId}")
  public TransferResponse get(@PathVariable("transferId") UUID transferId) {
    return service.get(transferId);
  }
}
