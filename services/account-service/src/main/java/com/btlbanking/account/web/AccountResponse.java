package com.btlbanking.account.web;

import java.math.BigDecimal;
import java.util.UUID;
import com.btlbanking.account.domain.AccountEntity;
import com.btlbanking.account.domain.AccountStatus;

public record AccountResponse(
    UUID id,
    String accountNumber,
    String ownerName,
    BigDecimal balance,
    AccountStatus status
) {
  public static AccountResponse from(AccountEntity entity) {
    return new AccountResponse(
        entity.getId(),
        entity.getAccountNumber(),
        entity.getOwnerName(),
        entity.getBalance(),
        entity.getStatus());
  }
}
