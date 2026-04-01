package com.btlbanking.fraud.web;

import java.math.BigDecimal;

public record FraudCheckRequest(
    String sourceAccount,
    String destinationAccount,
    BigDecimal amount
) {}