package com.btlbanking.account.service;

import java.math.BigDecimal;
import com.btlbanking.account.domain.AccountEntity;
import com.btlbanking.account.domain.AccountRepository;
import com.btlbanking.account.domain.AccountStatus;
import com.btlbanking.account.web.AccountResponse;
import com.btlbanking.account.web.BalanceCommand;
import com.btlbanking.account.web.CreateAccountRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class AccountCommandService {

  private final AccountRepository repository;

  public AccountCommandService(AccountRepository repository) {
    this.repository = repository;
  }

  public AccountResponse create(CreateAccountRequest request) {
    if (repository.existsByAccountNumber(request.accountNumber())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Account number already exists");
    }

    AccountEntity account = new AccountEntity();
    account.setAccountNumber(request.accountNumber());
    account.setOwnerName(request.ownerName());
    account.setBalance(request.balance());
    account.setStatus(AccountStatus.ACTIVE);
    return AccountResponse.from(repository.save(account));
  }

  @Transactional(readOnly = true)
  public AccountResponse get(String accountNumber) {
    return AccountResponse.from(findByAccountNumber(accountNumber));
  }

  @Transactional(readOnly = true)
  public AccountResponse getByOwnerName(String ownerName) {
    return AccountResponse.from(repository.findFirstByOwnerNameIgnoreCase(ownerName)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found")));
  }

  public AccountResponse debit(BalanceCommand request) {
    AccountEntity account = findByAccountNumber(request.accountNumber());
    BigDecimal updatedBalance = account.getBalance().subtract(request.amount());
    if (updatedBalance.signum() < 0) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Insufficient funds");
    }
    account.setBalance(updatedBalance);
    account.setStatus(AccountStatus.ACTIVE);
    return AccountResponse.from(repository.save(account));
  }

  public AccountResponse credit(BalanceCommand request) {
    return adjustBalance(request.accountNumber(), request.amount());
  }

  private AccountResponse adjustBalance(String accountNumber, BigDecimal delta) {
    AccountEntity account = findByAccountNumber(accountNumber);
    account.setBalance(account.getBalance().add(delta));
    account.setStatus(AccountStatus.ACTIVE);
    return AccountResponse.from(repository.save(account));
  }

  private AccountEntity findByAccountNumber(String accountNumber) {
    return repository.findByAccountNumber(accountNumber)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));
  }
}
