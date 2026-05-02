package com.btlbanking.account.web;

import jakarta.validation.constraints.NotBlank;

public record CreateAccountRequest(
    @NotBlank String ownerName
) {
}
