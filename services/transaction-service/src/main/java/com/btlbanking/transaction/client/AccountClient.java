package com.btlbanking.transaction.client;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class AccountClient {

  public void debit(String accountNumber, BigDecimal amount) {
    throw new UnsupportedOperationException("Account client is not wired yet");
  }

  public void credit(String accountNumber, BigDecimal amount) {
    throw new UnsupportedOperationException("Account client is not wired yet");
  }

  public void compensate(String accountNumber, BigDecimal amount) {
    throw new UnsupportedOperationException("Account client is not wired yet");
  }
}
