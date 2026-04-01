package com.btlbanking.transaction.service;

import com.btlbanking.transaction.domain.TransferEntity;
import com.btlbanking.transaction.domain.TransferRepository;
import com.btlbanking.transaction.domain.TransferStatus;
import com.btlbanking.transaction.web.CreateTransferRequest;
import com.btlbanking.transaction.web.TransferResponse;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TransferApplicationService {

  private final TransferRepository repository;

  public TransferApplicationService(TransferRepository repository) {
    this.repository = repository;
  }

  public TransferResponse create(String userId, String idempotencyKey, CreateTransferRequest request) {
    TransferEntity entity = new TransferEntity();
    entity.setUserId(userId);
    entity.setIdempotencyKey(idempotencyKey);
    entity.setSourceAccount(request.sourceAccount());
    entity.setDestinationAccount(request.destinationAccount());
    entity.setAmount(request.amount());
    entity.setStatus(TransferStatus.PENDING);

    return toResponse(repository.save(entity));
  }

  @Transactional(readOnly = true)
  public TransferResponse get(UUID transferId) {
    TransferEntity entity = repository.findById(transferId)
        .orElseThrow(() -> new IllegalArgumentException("Transfer not found: " + transferId));
    return toResponse(entity);
  }

  private TransferResponse toResponse(TransferEntity entity) {
    return new TransferResponse(entity.getId(), entity.getStatus());
  }
}
