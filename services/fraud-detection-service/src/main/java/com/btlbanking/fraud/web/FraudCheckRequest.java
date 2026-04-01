package com.btlbanking.fraud.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record FraudCheckRequest(
    @NotBlank String sourceAccount,
    @NotBlank String destinationAccount,
    @NotNull @Positive BigDecimal amount
) {}