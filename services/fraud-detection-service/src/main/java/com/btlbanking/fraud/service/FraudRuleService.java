package com.btlbanking.fraud.service;

import com.btlbanking.fraud.web.FraudCheckRequest;
import com.btlbanking.fraud.web.FraudCheckResponse;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.annotation.Observed;
import java.math.BigDecimal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class FraudRuleService {
  private static final Logger log = LoggerFactory.getLogger(FraudRuleService.class);

  private static final BigDecimal AMOUNT_LIMIT = new BigDecimal("100000000");
  private final MeterRegistry meterRegistry;

  public FraudRuleService(MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
  }

  @Observed(name = "banking.fraud.evaluate", contextualName = "fraud-evaluate")
  public FraudCheckResponse evaluate(FraudCheckRequest request) {
    if (request.sourceAccount().equals(request.destinationAccount())) {
      meterRegistry.counter("banking.fraud.decisions", "decision", "REJECTED", "reason", "SOURCE_EQUALS_DESTINATION").increment();
      log.info("Fraud decision=REJECTED reason=SOURCE_EQUALS_DESTINATION sourceAccount={} destinationAccount={}",
          request.sourceAccount(), request.destinationAccount());
      return new FraudCheckResponse("REJECTED", "SOURCE_EQUALS_DESTINATION");
    }
    if (request.amount().compareTo(AMOUNT_LIMIT) > 0) {
      meterRegistry.counter("banking.fraud.decisions", "decision", "REJECTED", "reason", "AMOUNT_LIMIT_EXCEEDED").increment();
      log.info("Fraud decision=REJECTED reason=AMOUNT_LIMIT_EXCEEDED amount={}", request.amount());
      return new FraudCheckResponse("REJECTED", "AMOUNT_LIMIT_EXCEEDED");
    }
    meterRegistry.counter("banking.fraud.decisions", "decision", "APPROVED", "reason", "OK").increment();
    log.info("Fraud decision=APPROVED sourceAccount={} destinationAccount={} amount={}",
        request.sourceAccount(), request.destinationAccount(), request.amount());
    return new FraudCheckResponse("APPROVED", "OK");
  }
}
