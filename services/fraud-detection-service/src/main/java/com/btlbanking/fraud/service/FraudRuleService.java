package com.btlbanking.fraud.service;

import com.btlbanking.fraud.web.FraudCheckRequest;
import com.btlbanking.fraud.web.FraudCheckResponse;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;

@Service
public class FraudRuleService {

  private static final BigDecimal AMOUNT_LIMIT = new BigDecimal("100000000");

  public FraudCheckResponse evaluate(FraudCheckRequest request) {
    if (request.sourceAccount().equals(request.destinationAccount())) {
      return new FraudCheckResponse("REJECTED", "SOURCE_EQUALS_DESTINATION");
    }
    if (request.amount().compareTo(AMOUNT_LIMIT) > 0) {
      return new FraudCheckResponse("REJECTED", "AMOUNT_LIMIT_EXCEEDED");
    }
    return new FraudCheckResponse("APPROVED", "OK");
  }
}