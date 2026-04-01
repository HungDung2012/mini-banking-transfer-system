package com.btlbanking.account.web;

import java.math.BigDecimal;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record CreateAccountRequest(
    @NotBlank String accountNumber,
    @NotBlank String ownerName,
    @NotNull @PositiveOrZero BigDecimal balance
) {
}
