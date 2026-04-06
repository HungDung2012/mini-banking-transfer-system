package com.btlbanking.account.web;

import com.btlbanking.account.service.AccountCommandService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/accounts")
public class AccountController {

  private final AccountCommandService service;

  public AccountController(AccountCommandService service) {
    this.service = service;
  }

  @PostMapping
  public ResponseEntity<AccountResponse> create(@Valid @RequestBody CreateAccountRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
  }

  @GetMapping("/{accountNumber}")
  public AccountResponse get(@PathVariable("accountNumber") String accountNumber) {
    return service.get(accountNumber);
  }

  @PostMapping("/debit")
  public AccountResponse debit(@Valid @RequestBody BalanceCommand request) {
    return service.debit(request);
  }

  @PostMapping("/credit")
  public AccountResponse credit(@Valid @RequestBody BalanceCommand request) {
    return service.credit(request);
  }

  @PostMapping("/compensate")
  public AccountResponse compensate(@Valid @RequestBody BalanceCommand request) {
    return service.credit(request);
  }
}
