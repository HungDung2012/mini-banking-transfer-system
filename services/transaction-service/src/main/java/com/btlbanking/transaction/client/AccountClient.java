package com.btlbanking.transaction.client;

import java.math.BigDecimal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class AccountClient {

  private final RestClient restClient;

  public AccountClient(@Value("${integration.account-service.base-url:http://localhost:8082}") String baseUrl) {
    this.restClient = RestClient.builder()
        .baseUrl(baseUrl)
        .build();
  }

  public void debit(String accountNumber, BigDecimal amount) {
    sendBalanceCommand("/accounts/debit", accountNumber, amount);
  }

  public void credit(String accountNumber, BigDecimal amount) {
    sendBalanceCommand("/accounts/credit", accountNumber, amount);
  }

  public void compensate(String accountNumber, BigDecimal amount) {
    sendBalanceCommand("/accounts/compensate", accountNumber, amount);
  }

  private void sendBalanceCommand(String path, String accountNumber, BigDecimal amount) {
    restClient.post()
        .uri(path)
        .contentType(MediaType.APPLICATION_JSON)
        .body(new BalanceCommand(accountNumber, amount))
        .retrieve()
        .toBodilessEntity();
  }
}
