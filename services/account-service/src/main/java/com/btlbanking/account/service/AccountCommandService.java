package com.btlbanking.account.service;

import java.math.BigDecimal;
import com.btlbanking.account.domain.AccountEntity;
import com.btlbanking.account.domain.AccountRepository;
import com.btlbanking.account.domain.AccountStatus;
import com.btlbanking.account.web.AccountResponse;
import com.btlbanking.account.web.BalanceCommand;
import com.btlbanking.account.web.CreateAccountRequest;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.annotation.Observed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class AccountCommandService {
  private static final Logger log = LoggerFactory.getLogger(AccountCommandService.class);

  private final AccountRepository repository;
  private final MeterRegistry meterRegistry;

  public AccountCommandService(AccountRepository repository, MeterRegistry meterRegistry) {
    this.repository = repository;
    this.meterRegistry = meterRegistry;
  }

  @Observed(name = "banking.account.create", contextualName = "account-create")
  public AccountResponse create(CreateAccountRequest request) {
    if (repository.existsByAccountNumber(request.accountNumber())) {
      meterRegistry.counter("banking.account.create.failures", "reason", "duplicate-account").increment();
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Account number already exists");
    }

    AccountEntity account = new AccountEntity();
    account.setAccountNumber(request.accountNumber());
    account.setOwnerName(request.ownerName());
    account.setBalance(request.balance());
    account.setStatus(AccountStatus.ACTIVE);
    AccountResponse response = AccountResponse.from(repository.save(account));
    meterRegistry.counter("banking.account.create.success").increment();
    log.info("Created account accountNumber={} owner={}", response.accountNumber(), response.ownerName());
    return response;
  }

  @Transactional(readOnly = true)
  @Observed(name = "banking.account.get", contextualName = "account-get")
  public AccountResponse get(String accountNumber) {
    return AccountResponse.from(findByAccountNumber(accountNumber));
  }

  @Transactional(readOnly = true)
  @Observed(name = "banking.account.get-by-owner", contextualName = "account-get-by-owner")
  public AccountResponse getByOwnerName(String ownerName) {
    return AccountResponse.from(repository.findFirstByOwnerNameIgnoreCase(ownerName)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found")));
  }

  @Observed(name = "banking.account.debit", contextualName = "account-debit")
  public AccountResponse debit(BalanceCommand request) {
    AccountEntity account = findByAccountNumber(request.accountNumber());
    BigDecimal updatedBalance = account.getBalance().subtract(request.amount());
    if (updatedBalance.signum() < 0) {
      meterRegistry.counter("banking.account.debit.failures", "reason", "insufficient-funds").increment();
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Insufficient funds");
    }
    account.setBalance(updatedBalance);
    account.setStatus(AccountStatus.ACTIVE);
    AccountResponse response = AccountResponse.from(repository.save(account));
    meterRegistry.counter("banking.account.debit.success").increment();
    log.info("Debited account accountNumber={} amount={} balance={}", response.accountNumber(),
        request.amount(), response.balance());
    return response;
  }

  @Observed(name = "banking.account.credit", contextualName = "account-credit")
  public AccountResponse credit(BalanceCommand request) {
    return adjustBalance(request.accountNumber(), request.amount());
  }

  private AccountResponse adjustBalance(String accountNumber, BigDecimal delta) {
    AccountEntity account = findByAccountNumber(accountNumber);
    account.setBalance(account.getBalance().add(delta));
    account.setStatus(AccountStatus.ACTIVE);
    AccountResponse response = AccountResponse.from(repository.save(account));
    meterRegistry.counter("banking.account.credit.success").increment();
    log.info("Credited account accountNumber={} delta={} balance={}", response.accountNumber(),
        delta, response.balance());
    return response;
  }

  private AccountEntity findByAccountNumber(String accountNumber) {
    return repository.findByAccountNumber(accountNumber)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));
  }
}
