package com.btlbanking.fraud.web;

import com.btlbanking.fraud.service.FraudRuleService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/fraud")
public class FraudController {

  private final FraudRuleService fraudRuleService;

  public FraudController(FraudRuleService fraudRuleService) {
    this.fraudRuleService = fraudRuleService;
  }

  @PostMapping("/check")
  public FraudCheckResponse check(@Valid @RequestBody FraudCheckRequest request) {
    return fraudRuleService.evaluate(request);
  }
}