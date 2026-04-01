package com.btlbanking.account.web;

import java.math.BigDecimal;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record BalanceCommand(
    @NotBlank String accountNumber,
    @NotNull @Positive BigDecimal amount
) {
}
