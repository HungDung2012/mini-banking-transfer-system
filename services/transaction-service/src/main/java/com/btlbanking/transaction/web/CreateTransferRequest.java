package com.btlbanking.transaction.web;

import java.math.BigDecimal;

public record CreateTransferRequest(String sourceAccount, String destinationAccount, BigDecimal amount) {
}
