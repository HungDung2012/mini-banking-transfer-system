package com.btlbanking.transaction.client;

import com.btlbanking.transaction.web.CreateTransferRequest;
import org.springframework.stereotype.Component;

@Component
public class FraudClient {

  public FraudCheckResponse check(CreateTransferRequest request) {
    throw new UnsupportedOperationException("Fraud client is not wired yet");
  }
}
